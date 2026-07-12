package io.github.lxien.orbien.client.filetransfer;

import io.github.lxien.orbien.client.manager.ProxyManager;
import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.core.filetransfer.FileTransferCompressionSupport;
import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;

public class FileTransferControlHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FileTransferControlHandler.class);

    private final ProxyManager proxyManager = ProxyManagerHolder.get();
    private final FileSystemService fileSystemService = new FileSystemService();
    private final FileTransferSessionManager sessionManager = new FileTransferSessionManager();

    public void handle(Channel control, TMSPFrame frame) {
        try {
            switch (frame.getMsgType()) {
                case TMSP.MSG_FILE_LIST_REQ -> handleList(control, frame);
                case TMSP.MSG_FILE_TRANSFER_INIT -> handleTransferInit(control, frame);
                case TMSP.MSG_FILE_CHUNK -> handleChunk(control, frame);
                case TMSP.MSG_FILE_TRANSFER_DONE -> handleTransferDone(control, frame);
                case TMSP.MSG_FILE_OP_REQ -> handleOp(control, frame);
                default -> logger.warn("未知文件消息: {}", frame.getMsgType());
            }
        } catch (FilePermissionChecker.FileAccessException e) {
            logger.debug("文件访问拒绝: {}", e.getMessage());
            failFromFrame(control, frame, e.getMessage());
        } catch (Exception e) {
            logger.error("处理文件消息失败", e);
            failFromFrame(control, frame, "内部错误");
        }
    }

    private <T extends com.google.protobuf.Message> T parseFrame(Channel control, TMSPFrame frame,
                                                                 com.google.protobuf.Parser<T> parser) {
        TmspPayloadCompressor.ControlPayload payload =
                TmspPayloadCompressor.decodeControlPayload(control, frame);
        try {
            return ProtobufUtil.parseFrom(payload.buf(), parser);
        } finally {
            payload.releaseIfOwned();
        }
    }

    private void handleList(Channel control, TMSPFrame frame) throws Exception {
        Message.FileListRequest req = parseFrame(control, frame, Message.FileListRequest.parser());
        logger.debug("收到文件列表请求 proxyId={} path={}", req.getProxyId(), req.getPath());
        PathContext ctx = resolve(req.getProxyId());
        Message.FileListResponse resp = fileSystemService.list(ctx.root(), req.getPath(), req.getSort());
        send(control, TMSP.MSG_FILE_LIST_RESP, resp.toBuilder().setRequestId(req.getRequestId()).build(), req.getProxyId());
    }

    private void handleTransferInit(Channel control, TMSPFrame frame) throws Exception {
        Message.FileTransferInit req = parseFrame(control, frame, Message.FileTransferInit.parser());
        PathContext ctx = resolve(req.getProxyId());
        if (req.getUpload()) {
            FilePermissionChecker.assertWritable(ctx.limits());
            long max = FilePermissionChecker.maxUploadSize(ctx.limits());
            if (req.getTotalSize() > max) {
                failDone(control, req.getRequestId(), req.getProxyId(), "文件超过大小限制");
                return;
            }
            sessionManager.startUpload(req.getRequestId(), req.getProxyId(), req.getPath());
        } else {
            InputStream in = fileSystemService.openDownload(ctx.root(), req.getPath());
            long maxBytes = req.getMaxBytes();
            sendChunks(control, req.getRequestId(), req.getProxyId(), in, maxBytes);
        }
    }

    private void handleChunk(Channel control, TMSPFrame frame) throws Exception {
        Message.FileChunk chunk = parseFrame(control, frame, Message.FileChunk.parser());
        FileTransferSessionManager.UploadSession session = sessionManager.upload(chunk.getRequestId());
        if (session == null) {
            failDone(control, chunk.getRequestId(), null, "上传会话不存在");
            return;
        }
        PathContext ctx = resolve(session.getProxyId());
        byte[] data = chunk.getData().toByteArray();
        try {
            fileSystemService.writeChunk(ctx.root(), session.getPath(), chunk.getOffset(), data, chunk.getLast(), ctx.limits());
        } catch (Exception e) {
            abortUpload(ctx.root(), session);
            throw e;
        }
        if (chunk.getLast()) {
            sessionManager.finishUpload(chunk.getRequestId());
            sendDone(control, chunk.getRequestId(), session.getProxyId());
        }
    }

    private void handleTransferDone(Channel control, TMSPFrame frame) {
        Message.FileTransferDone done = parseFrame(control, frame, Message.FileTransferDone.parser());
        sessionManager.finishUpload(done.getRequestId());
    }

    private void handleOp(Channel control, TMSPFrame frame) throws Exception {
        Message.FileOpRequest req = parseFrame(control, frame, Message.FileOpRequest.parser());
        PathContext ctx = resolve(req.getProxyId());
        Message.FileOpResponse resp;
        if (FileTransferConstants.OP_MKDIR.equals(req.getOp())) {
            resp = fileSystemService.mkdir(ctx.root(), req.getPath(), req.getName(), ctx.limits());
        } else if (FileTransferConstants.OP_DELETE.equals(req.getOp())) {
            resp = fileSystemService.delete(ctx.root(), req.getPath(), ctx.limits());
        } else if (FileTransferConstants.OP_MOVE.equals(req.getOp())) {
            resp = fileSystemService.move(ctx.root(), req.getPath(), req.getName(), ctx.limits());
        } else if (FileTransferConstants.OP_RENAME.equals(req.getOp())) {
            resp = fileSystemService.rename(ctx.root(), req.getPath(), req.getName(), ctx.limits());
        } else {
            resp = Message.FileOpResponse.newBuilder()
                    .setStatus(status(1, "不支持的操作"))
                    .build();
        }
        send(control, TMSP.MSG_FILE_OP_RESP, resp.toBuilder().setRequestId(req.getRequestId()).build(), req.getProxyId());
    }

    private void sendChunks(Channel control, String requestId, String proxyId, InputStream in,
                            long maxBytes) throws Exception {
        byte[] buffer = new byte[FileTransferConstants.CHUNK_SIZE];
        long offset = 0;
        try (PushbackInputStream pin = new PushbackInputStream(in, 1)) {
            int read;
            while (true) {
                read = pin.read(buffer);
                if (read == -1) {
                    break;
                }
                if (maxBytes > 0 && offset >= maxBytes) {
                    break;
                }
                int toSend = read;
                boolean last;
                if (maxBytes > 0 && offset + read > maxBytes) {
                    toSend = (int) (maxBytes - offset);
                    last = true;
                } else {
                    int next = pin.read();
                    last = next == -1;
                    if (next != -1) {
                        pin.unread(next);
                    }
                    if (maxBytes > 0 && offset + toSend >= maxBytes) {
                        last = true;
                    }
                }
                Message.FileChunk chunk = Message.FileChunk.newBuilder()
                        .setRequestId(requestId)
                        .setOffset(offset)
                        .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, toSend))
                        .setLast(last)
                        .build();
                send(control, TMSP.MSG_FILE_CHUNK, chunk, proxyId);
                offset += toSend;
                if (last) {
                    break;
                }
            }
        }
        sendDone(control, requestId, proxyId);
    }

    private PathContext resolve(String proxyId) {
        Message.RuntimeInfo info = proxyManager.get(proxyId);
        if (info == null || !info.hasFileLimits() || info.getFileLimits().getRootPath().isEmpty()) {
            throw new FilePermissionChecker.FileAccessException("文件共享未配置");
        }
        return new PathContext(Path.of(info.getFileLimits().getRootPath()), info.getFileLimits());
    }

    private void failFromFrame(Channel control, TMSPFrame frame, String message) {
        byte type = frame.getMsgType();
        try {
            if (type == TMSP.MSG_FILE_LIST_REQ) {
                Message.FileListRequest req = parseFrame(control, frame, Message.FileListRequest.parser());
                Message.FileListResponse resp = Message.FileListResponse.newBuilder()
                        .setRequestId(req.getRequestId())
                        .setStatus(status(1, message))
                        .build();
                send(control, TMSP.MSG_FILE_LIST_RESP, resp, req.getProxyId());
            } else if (type == TMSP.MSG_FILE_TRANSFER_INIT) {
                Message.FileTransferInit req = parseFrame(control, frame, Message.FileTransferInit.parser());
                if (req.getUpload()) {
                    sessionManager.finishUpload(req.getRequestId());
                }
                failDone(control, req.getRequestId(), req.getProxyId(), message);
            } else if (type == TMSP.MSG_FILE_CHUNK) {
                Message.FileChunk chunk = parseFrame(control, frame, Message.FileChunk.parser());
                FileTransferSessionManager.UploadSession session = sessionManager.upload(chunk.getRequestId());
                String proxyId = session != null ? session.getProxyId() : null;
                if (session != null) {
                    try {
                        PathContext ctx = resolve(session.getProxyId());
                        abortUpload(ctx.root(), session);
                    } catch (Exception e) {
                        logger.debug("清理上传会话失败: {}", e.getMessage());
                    }
                } else {
                    sessionManager.finishUpload(chunk.getRequestId());
                }
                failDone(control, chunk.getRequestId(), proxyId, message);
            } else if (type == TMSP.MSG_FILE_OP_REQ) {
                Message.FileOpRequest req = parseFrame(control, frame, Message.FileOpRequest.parser());
                Message.FileOpResponse resp = Message.FileOpResponse.newBuilder()
                        .setRequestId(req.getRequestId())
                        .setStatus(status(1, message))
                        .build();
                send(control, TMSP.MSG_FILE_OP_RESP, resp, req.getProxyId());
            } else {
                logger.warn("无法回复文件错误响应 msgType={} message={}", type, message);
            }
        } catch (Exception e) {
            logger.warn("发送文件错误响应失败 msgType={} message={}", type, message, e);
        }
    }

    private void abortUpload(Path root, FileTransferSessionManager.UploadSession session) {
        sessionManager.finishUpload(session.getRequestId());
        fileSystemService.cleanupPartialUpload(root, session.getPath());
    }

    private void send(Channel control, byte type, com.google.protobuf.MessageLite message, String proxyId) {
        control.eventLoop().execute(() -> {
            ByteBuf buf = ProtobufUtil.toByteBuf(message, control.alloc());
            TMSPFrame frame = new TMSPFrame(0, type, buf);
            TmspPayloadCompressor.encodeControlPayload(control, frame, resolveAlgorithm(proxyId));
            control.writeAndFlush(frame);
        });
    }

    private CompressionType resolveAlgorithm(String proxyId) {
        Message.RuntimeInfo info = proxyId != null ? proxyManager.get(proxyId) : null;
        return FileTransferCompressionSupport.resolveFromRuntime(info);
    }

    private void sendDone(Channel control, String requestId, String proxyId) {
        send(control, TMSP.MSG_FILE_TRANSFER_DONE, Message.FileTransferDone.newBuilder()
                .setRequestId(requestId)
                .setStatus(status(0, "ok"))
                .build(), proxyId);
    }

    private void failDone(Channel control, String requestId, String proxyId, String message) {
        send(control, TMSP.MSG_FILE_TRANSFER_DONE, Message.FileTransferDone.newBuilder()
                .setRequestId(requestId)
                .setStatus(status(1, message))
                .build(), proxyId);
    }

    private Message.Status status(int code, String message) {
        return Message.Status.newBuilder().setCode(code).setMessage(message).build();
    }

    private record PathContext(Path root, Message.FileShareLimits limits) {
    }
}
