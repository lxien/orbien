package io.github.lxien.orbien.server.filetransfer;

import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class FileTransferCoordinator {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FileTransferCoordinator.class);

    @Autowired
    private AgentManager agentManager;

    private final Map<String, PendingRequest<?>> pending = new ConcurrentHashMap<>();
    private final Map<String, ChunkListener> chunkListeners = new ConcurrentHashMap<>();

    public Message.FileListResponse list(String agentId, String proxyId, String path) throws Exception {
        String requestId = newRequestId();
        Message.FileListRequest req = Message.FileListRequest.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setPath(path == null ? "/" : path)
                .build();
        CompletableFuture<Message.FileListResponse> future = register(requestId);
        send(agentId, TMSP.MSG_FILE_LIST_REQ, req, future);
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
        send(agentId, TMSP.MSG_FILE_TRANSFER_INIT, init, future);
        return new UploadSession(requestId, future, totalSize);
    }

    public void uploadChunk(String agentId, String requestId, long offset, byte[] data, boolean last) throws Exception {
        Message.FileChunk chunk = Message.FileChunk.newBuilder()
                .setRequestId(requestId)
                .setOffset(offset)
                .setData(com.google.protobuf.ByteString.copyFrom(data))
                .setLast(last)
                .build();
        sendAndAwait(agentId, TMSP.MSG_FILE_CHUNK, chunk, FileTransferConstants.CHUNK_SEND_TIMEOUT_MS);
    }

    public void awaitUpload(UploadSession session) throws Exception {
        long timeoutMs = FileTransferConstants.transferTimeoutMs(session.totalSize());
        Message.FileTransferDone done = session.future().get(timeoutMs, TimeUnit.MILLISECONDS);
        if (done.getStatus().getCode() != 0) {
            throw new IllegalStateException(done.getStatus().getMessage());
        }
    }

    public byte[] download(String agentId, String proxyId, String path) throws Exception {
        String requestId = newRequestId();
        List<byte[]> chunks = Collections.synchronizedList(new ArrayList<>());
        registerChunkListener(requestId, chunk -> chunks.add(chunk.getData().toByteArray()));
        Message.FileTransferInit init = Message.FileTransferInit.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setPath(path)
                .setUpload(false)
                .build();
        CompletableFuture<Message.FileTransferDone> future = register(requestId,
                FileTransferConstants.transferTimeoutMs(FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE));
        send(agentId, TMSP.MSG_FILE_TRANSFER_INIT, init, future);
        Message.FileTransferDone done = future.get(
                FileTransferConstants.transferTimeoutMs(FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE),
                TimeUnit.MILLISECONDS);
        removeChunkListener(requestId);
        if (done.getStatus().getCode() != 0) {
            throw new IllegalStateException(done.getStatus().getMessage());
        }
        int total = chunks.stream().mapToInt(c -> c.length).sum();
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    public Message.FileOpResponse op(String agentId, String proxyId, String op, String path, String name) throws Exception {
        String requestId = newRequestId();
        Message.FileOpRequest req = Message.FileOpRequest.newBuilder()
                .setRequestId(requestId)
                .setProxyId(proxyId)
                .setOp(op)
                .setPath(path)
                .setName(name == null ? "" : name)
                .build();
        CompletableFuture<Message.FileOpResponse> future = register(requestId);
        send(agentId, TMSP.MSG_FILE_OP_REQ, req, future);
        return future.get(FileTransferConstants.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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

    public interface ChunkListener {
        void onChunk(Message.FileChunk chunk);
    }

    public record UploadSession(String requestId, CompletableFuture<Message.FileTransferDone> future, long totalSize) {
    }

    private void sendAndAwait(String agentId, byte type, com.google.protobuf.MessageLite message, long timeoutMs) throws Exception {
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
            ByteBuf buf = ProtobufUtil.toByteBuf(message, control.alloc());
            control.writeAndFlush(new TMSPFrame(0, type, buf))
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

    private void send(String agentId, byte type, com.google.protobuf.MessageLite message) {
        send(agentId, type, message, null);
    }

    private void send(String agentId, byte type, com.google.protobuf.MessageLite message,
                      CompletableFuture<?> pendingFuture) {
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
            ByteBuf buf = ProtobufUtil.toByteBuf(message, control.alloc());
            control.writeAndFlush(new TMSPFrame(0, type, buf))
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
