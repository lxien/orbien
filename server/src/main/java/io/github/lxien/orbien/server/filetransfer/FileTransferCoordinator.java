package io.github.lxien.orbien.server.filetransfer;

import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.filetransfer.FileTransferCompressionSupport;
import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FileTransferCoordinator {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FileTransferCoordinator.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProxyConfigService proxyConfigService;

    private final Map<String, PendingRequest<?>> pending = new ConcurrentHashMap<>();
    private final Map<String, ChunkListener> chunkListeners = new ConcurrentHashMap<>();
    private final Map<String, String> transferProxyIds = new ConcurrentHashMap<>();

    public Message.FileListResponse list(String agentId, String proxyId, String path, String sort) throws Exception {
        String requestId = newRequestId();
        Message.FileListRequest req = Message.FileListRequest.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setPath(path == null ? "/" : path)
                .setSort(sort == null ? "" : sort)
                .build();
        CompletableFuture<Message.FileListResponse> future = register(requestId);
        send(agentId, TMSP.MSG_FILE_LIST_REQ, req, proxyId, future);
        return future.get(FileTransferConstants.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public UploadSession startUpload(String agentId, String proxyId, String path, long totalSize) {
        String requestId = newRequestId();
        long timeoutMs = FileTransferConstants.transferTimeoutMs(totalSize);
        CompletableFuture<Message.FileTransferDone> future = register(requestId, timeoutMs);
        Message.FileTransferInit init = Message.FileTransferInit.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setPath(path)
                .setTotalSize(totalSize)
                .setUpload(true)
                .build();
        transferProxyIds.put(requestId, proxyId);
        send(agentId, TMSP.MSG_FILE_TRANSFER_INIT, init, proxyId, future);
        return new UploadSession(requestId, future, totalSize);
    }

    public void uploadChunk(String agentId, String requestId, long offset, byte[] data, boolean last) throws Exception {
        uploadChunk(agentId, requestId, offset, data, 0, data == null ? 0 : data.length, last);
    }

    public void uploadChunk(String agentId, String requestId, long offset, byte[] data, int dataOffset, int length,
                            boolean last) throws Exception {
        Message.FileChunk chunk = Message.FileChunk.newBuilder()
                .setRequestId(requestId)
                .setOffset(offset)
                .setData(com.google.protobuf.ByteString.copyFrom(data, dataOffset, length))
                .setLast(last)
                .build();
        sendAndAwait(agentId, TMSP.MSG_FILE_CHUNK, chunk,
                transferProxyIds.get(requestId), FileTransferConstants.CHUNK_SEND_TIMEOUT_MS);
    }

    public void awaitUpload(UploadSession session) throws Exception {
        long timeoutMs = FileTransferConstants.transferTimeoutMs(session.totalSize());
        Message.FileTransferDone done = session.future().get(timeoutMs, TimeUnit.MILLISECONDS);
        if (done.getStatus().getCode() != 0) {
            throw new IllegalStateException(done.getStatus().getMessage());
        }
    }

    /**
     * 下载并拼成完整字节数组。仅用于已限制大小的场景
     */
    public byte[] download(String agentId, String proxyId, String path, long maxBytes) throws Exception {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("download(byte[]) 必须指定 maxBytes");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(maxBytes, 64 * 1024));
        download(agentId, proxyId, path, maxBytes, maxBytes, out::write);
        return out.toByteArray();
    }

    /**
     * 流式下载
     */
    public void download(String agentId, String proxyId, String path, long maxBytes, long expectedBytes,
                         ChunkHandler handler) throws Exception {
        String requestId = newRequestId();
        // HTTP 写出慢时 offer 等待，配合 client 背压避免堆上堆积分块
        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(2);
        Object end = new Object();
        AtomicReference<Exception> handoffError = new AtomicReference<>();

        Message.FileTransferInit.Builder initBuilder = Message.FileTransferInit.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setPath(path)
                .setUpload(false);
        if (maxBytes > 0) {
            initBuilder.setMaxBytes(maxBytes);
        }
        transferProxyIds.put(requestId, proxyId);
        long timeoutBytes = expectedBytes > 0 ? expectedBytes
                : (maxBytes > 0 ? maxBytes : FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE);
        long timeoutMs = FileTransferConstants.transferTimeoutMs(timeoutBytes);
        CompletableFuture<Message.FileTransferDone> future = register(requestId, timeoutMs);

        registerChunkListener(requestId, chunk -> {
            try {
                if (!queue.offer(chunk.getData().toByteArray(),
                        FileTransferConstants.CHUNK_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    TimeoutException te = new TimeoutException("下载写出超时");
                    handoffError.compareAndSet(null, te);
                    future.completeExceptionally(te);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handoffError.compareAndSet(null, e);
                future.completeExceptionally(e);
            }
        });
        future.whenComplete((done, err) -> {
            try {
                queue.put(end);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        send(agentId, TMSP.MSG_FILE_TRANSFER_INIT, initBuilder.build(), proxyId, future);

        try {
            while (true) {
                Object item = queue.poll(1, TimeUnit.SECONDS);
                Exception he = handoffError.get();
                if (he != null) {
                    throw he;
                }
                if (item == end) {
                    break;
                }
                if (item != null) {
                    handler.onChunk((byte[]) item);
                }
            }
            Message.FileTransferDone done = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (done.getStatus().getCode() != 0) {
                throw new IllegalStateException(done.getStatus().getMessage());
            }
        } finally {
            removeChunkListener(requestId);
            transferProxyIds.remove(requestId);
            queue.clear();
        }
    }

    public Message.FileOpResponse op(String agentId, String proxyId, String op, String path, String name) throws Exception {
        return op(agentId, proxyId, op, path, name, FileTransferConstants.REQUEST_TIMEOUT_MS);
    }

    public Message.FileOpResponse op(String agentId, String proxyId, String op, String path, String name,
                                     long timeoutMs) throws Exception {
        String requestId = newRequestId();
        Message.FileOpRequest req = Message.FileOpRequest.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setOp(op)
                .setPath(path)
                .setName(name == null ? "" : name)
                .build();
        CompletableFuture<Message.FileOpResponse> future = register(requestId, timeoutMs);
        send(agentId, TMSP.MSG_FILE_OP_REQ, req, proxyId, future);
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void onListResp(Message.FileListResponse resp) {
        logger.debug("收到文件列表响应 requestId={} code={}",
                resp.getRequestId(), resp.hasStatus() ? resp.getStatus().getCode() : -1);
        complete(resp.getRequestId(), resp);
    }

    public void onOpResp(Message.FileOpResponse resp) {
        complete(resp.getRequestId(), resp);
    }

    public void onTransferDone(Message.FileTransferDone done) {
        transferProxyIds.remove(done.getRequestId());
        complete(done.getRequestId(), done);
    }

    public void onChunk(Message.FileChunk chunk) {
        ChunkListener listener = chunkListeners.get(chunk.getRequestId());
        if (listener != null) {
            listener.onChunk(chunk);
        }
    }

    public void registerChunkListener(String requestId, ChunkListener listener) {
        chunkListeners.put(requestId, listener);
    }

    public void removeChunkListener(String requestId) {
        chunkListeners.remove(requestId);
    }

    @FunctionalInterface
    public interface ChunkHandler {
        void onChunk(byte[] data) throws Exception;
    }

    public interface ChunkListener {
        void onChunk(Message.FileChunk chunk);
    }

    public record UploadSession(String requestId, CompletableFuture<Message.FileTransferDone> future, long totalSize) {
    }

    private void sendAndAwait(String agentId, byte type, com.google.protobuf.MessageLite message,
                              String proxyId, long timeoutMs) throws Exception {
        Optional<AgentContext> contextOpt = agentManager.getAgentContext(agentId);
        if (contextOpt.isEmpty()) {
            throw new IllegalStateException("客户端 Agent 不在线: " + agentId);
        }
        AgentContext context = contextOpt.get();
        if (context.getState() != AgentState.CONNECTED) {
            throw new IllegalStateException("客户端 Agent 未就绪: " + agentId + ", state=" + context.getState());
        }
        Channel control = context.getControl();
        if (control == null || !control.isActive()) {
            throw new IllegalStateException("客户端控制通道不可用: " + agentId);
        }
        CompletableFuture<Void> writeFuture = new CompletableFuture<>();
        control.eventLoop().execute(() -> {
            TMSPFrame frame = buildFrame(control, type, message, proxyId);
            control.writeAndFlush(frame)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            logger.debug("已发送文件传输消息 agentId={} msgType={}", agentId, type);
                            writeFuture.complete(null);
                        } else {
                            logger.warn("文件传输消息发送失败 agentId={} msgType={}", agentId, type, future.cause());
                            writeFuture.completeExceptionally(new IllegalStateException("文件传输消息发送失败", future.cause()));
                        }
                    });
        });
        writeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void send(String agentId, byte type, com.google.protobuf.MessageLite message, String proxyId) {
        send(agentId, type, message, proxyId, null);
    }

    private void send(String agentId, byte type, com.google.protobuf.MessageLite message,
                      String proxyId, CompletableFuture<?> pendingFuture) {
        Optional<AgentContext> contextOpt = agentManager.getAgentContext(agentId);
        if (contextOpt.isEmpty()) {
            failPending(pendingFuture, "客户端 Agent 不在线: " + agentId);
            return;
        }
        AgentContext context = contextOpt.get();
        if (context.getState() != AgentState.CONNECTED) {
            failPending(pendingFuture, "客户端 Agent 未就绪: " + agentId + ", state=" + context.getState());
            return;
        }
        Channel control = context.getControl();
        if (control == null || !control.isActive()) {
            failPending(pendingFuture, "客户端控制通道不可用: " + agentId);
            return;
        }
        control.eventLoop().execute(() -> {
            TMSPFrame frame = buildFrame(control, type, message, proxyId);
            control.writeAndFlush(frame)
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            logger.warn("文件传输消息发送失败 agentId={} msgType={}", agentId, type, future.cause());
                            failPending(pendingFuture, "文件传输消息发送失败");
                        } else {
                            logger.debug("已发送文件传输消息 agentId={} msgType={}", agentId, type);
                        }
                    });
        });
    }

    private TMSPFrame buildFrame(Channel control, byte type, com.google.protobuf.MessageLite message, String proxyId) {
        ByteBuf buf = ProtobufUtil.toByteBuf(message, control.alloc());
        TMSPFrame frame = new TMSPFrame(0, type, buf);
        TmspPayloadCompressor.encodeControlPayload(control, frame, resolveAlgorithm(proxyId));
        return frame;
    }

    private CompressionType resolveAlgorithm(String proxyId) {
        if (proxyId == null || proxyId.isBlank()) {
            return CompressionType.NONE;
        }
        var ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            return CompressionType.NONE;
        }
        return FileTransferCompressionSupport.resolveFromProxy(ext.getProxyConfig());
    }

    private void failPending(CompletableFuture<?> future, String message) {
        logger.warn(message);
        if (future != null && !future.isDone()) {
            future.completeExceptionally(new IllegalStateException(message));
        }
    }

    private <T> CompletableFuture<T> register(String requestId) {
        return register(requestId, FileTransferConstants.REQUEST_TIMEOUT_MS);
    }

    private <T> CompletableFuture<T> register(String requestId, long timeoutMs) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.put(requestId, new PendingRequest<>(future));
        future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((r, ex) -> pending.remove(requestId));
        return future;
    }

    @SuppressWarnings("unchecked")
    private <T> void complete(String requestId, T result) {
        PendingRequest<?> pr = pending.remove(requestId);
        if (pr != null) {
            ((CompletableFuture<T>) pr.future()).complete(result);
        }
    }

    private String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record PendingRequest<T>(CompletableFuture<T> future) {
    }
}
