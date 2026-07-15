/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.transport.file;

import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.filetransfer.FileTransferCoordinator;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component
@ChannelHandler.Sharable
public class FileShareDispatchHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FileShareDispatchHandler.class);

    private static final AttributeKey<IngressState> INGRESS =
            AttributeKey.valueOf("fileShareHttpIngress");

    private static final long MAX_BUFFERED_REQUEST_BYTES = 4L * 1024 * 1024;
    private static final int MAX_HEADER_BYTES = FileShareHttpFrameBuffer.MAX_HEADER_BYTES;
    private static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private DomainRegistry domainRegistry;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private FileShareHttpHandler fileShareHttpHandler;
    @Autowired
    private FileTransferCoordinator fileTransferCoordinator;
    @Autowired
    private MetricsCollector metricsCollector;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        if (domain == null) {
            ctx.fireChannelRead(msg);
            return;
        }
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (proxyId == null) {
            ctx.fireChannelRead(msg);
            return;
        }
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || !ext.getProxyConfig().isFile()) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (!(msg instanceof ByteBuf incoming)) {
            ReferenceCountUtil.release(msg);
            return;
        }

        ProxyConfig config = ext.getProxyConfig();
        FileShareMetricsWriteHandler.ensureInstalled(ctx, metricsCollector, proxyId, config.getAgentType());

        try {
            IngressState state = visitor.attr(INGRESS).get();
            if (state == null) {
                state = new IngressState();
                visitor.attr(INGRESS).set(state);
            }
            switch (state.mode) {
                case READING_HEADERS -> feedHeaders(ctx, ext, visitor, state, incoming);
                case BUFFERED -> feedBuffered(ctx, ext, visitor, state, incoming);
                case STREAMING -> feedStreaming(visitor, state, config, incoming);
            }
        } catch (OutOfMemoryError oom) {
            logger.warn("文件共享请求内存不足，关闭连接 proxyId={}", config.getProxyId());
            ReferenceCountUtil.safeRelease(incoming);
            clearIngress(visitor);
            NettyHttpUtils.sendHttp413(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
        }
    }

    private void feedHeaders(ChannelHandlerContext ctx, ProxyConfigExt ext, Channel visitor,
                             IngressState state, ByteBuf incoming) {
        if (state.headerBuf == null) {
            state.headerBuf = Unpooled.buffer(Math.min(incoming.readableBytes() + 256, MAX_HEADER_BYTES));
        }
        if ((long) state.headerBuf.readableBytes() + incoming.readableBytes() > MAX_HEADER_BYTES) {
            ReferenceCountUtil.release(incoming);
            reject413(visitor);
            return;
        }
        state.headerBuf.writeBytes(incoming);
        ReferenceCountUtil.release(incoming);

        int headerEnd = indexOf(state.headerBuf, CRLFCRLF);
        if (headerEnd < 0) {
            if (state.headerBuf.readableBytes() >= MAX_HEADER_BYTES) {
                reject413(visitor);
            }
            return;
        }

        int headerBytes = headerEnd + 4;
        ParsedRequestLine parsed = parseRequestHeaders(state.headerBuf, headerBytes);
        if (parsed == null) {
            clearIngress(visitor);
            NettyHttpUtils.sendHttp400(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
            return;
        }

        ProxyConfig config = ext.getProxyConfig();
        long maxUpload = resolveMaxUploadBytes(config);
        if (parsed.contentLength > maxUpload) {
            reject413(visitor);
            return;
        }

        int bodyInBuf = state.headerBuf.readableBytes() - headerBytes;
        ByteBuf headerSlice = state.headerBuf.readRetainedSlice(headerBytes);
        ByteBuf leftover = bodyInBuf > 0 ? state.headerBuf.readRetainedSlice(bodyInBuf) : null;
        state.headerBuf.release();
        state.headerBuf = null;

        String pathOnly = stripQuery(parsed.path);
        if ("POST".equalsIgnoreCase(parsed.method) && pathOnly.startsWith("/api/files/upload")) {
            if (parsed.contentLength <= 0) {
                ReferenceCountUtil.release(headerSlice);
                ReferenceCountUtil.release(leftover);
                clearIngress(visitor);
                fileShareHttpHandler.writeJsonHttp(visitor, 400, Map.of("message", "无效的上传"));
                return;
            }
            startStreaming(visitor, state, ext, config, parsed, headerSlice, leftover);
            return;
        }

        startBuffered(ctx, ext, visitor, state, config, headerSlice, leftover, maxUpload);
    }

    private void startStreaming(Channel visitor, IngressState state, ProxyConfigExt ext, ProxyConfig config,
                                ParsedRequestLine parsed, ByteBuf headerSlice, ByteBuf leftover) {
        int headerByteCount = headerSlice.readableBytes();
        ReferenceCountUtil.release(headerSlice);
        FileShareHttpHandler.PreparedUpload prepared;
        try {
            prepared = fileShareHttpHandler.prepareUpload(ext, parsed.path, parsed.cookie);
        } catch (FileAuthService.AuthException e) {
            ReferenceCountUtil.release(leftover);
            clearIngress(visitor);
            fileShareHttpHandler.writeJsonHttp(visitor, 401, Map.of("message", e.getMessage()));
            return;
        } catch (FileShareHttpHandler.UploadDeniedException e) {
            ReferenceCountUtil.release(leftover);
            clearIngress(visitor);
            fileShareHttpHandler.writeJsonHttp(visitor, e.status(), Map.of("message", e.getMessage()));
            return;
        }

        FileShareMultipartUploader uploader = new FileShareMultipartUploader(
                fileTransferCoordinator, prepared, parsed.contentType, parsed.contentLength);
        if (uploader.phase() == FileShareMultipartUploader.Phase.ERROR) {
            ReferenceCountUtil.release(leftover);
            clearIngress(visitor);
            fileShareHttpHandler.writeJsonHttp(visitor, 400, Map.of("message", uploader.errorMessage()));
            return;
        }

        state.mode = Mode.STREAMING;
        state.uploader = uploader;
        state.proxyConfig = config;
        if (headerByteCount > 0) {
            metricsCollector.recordInbound(config.getProxyId(), config.getAgentType(), headerByteCount);
        }

        if (leftover != null) {
            feedStreaming(visitor, state, config, leftover);
        }
    }

    private void startBuffered(ChannelHandlerContext ctx, ProxyConfigExt ext, Channel visitor,
                               IngressState state, ProxyConfig config,
                               ByteBuf headerSlice, ByteBuf leftover, long maxUpload) {
        long bufferedCap = Math.min(maxUpload, MAX_BUFFERED_REQUEST_BYTES);
        bufferedCap = Math.max(MAX_HEADER_BYTES, bufferedCap);
        state.mode = Mode.BUFFERED;
        state.frameBuffer = new FileShareHttpFrameBuffer(bufferedCap);
        state.proxyConfig = config;

        ByteBuf first = leftover == null ? headerSlice : Unpooled.wrappedBuffer(headerSlice, leftover);
        feedBuffered(ctx, ext, visitor, state, first);
    }

    private void feedBuffered(ChannelHandlerContext ctx, ProxyConfigExt ext, Channel visitor,
                              IngressState state, ByteBuf incoming) {
        ProxyConfig config = state.proxyConfig != null ? state.proxyConfig : ext.getProxyConfig();
        ByteBuf chunk = incoming;
        while (true) {
            ByteBuf complete = state.frameBuffer.feed(visitor, chunk);
            chunk = null;
            if (state.frameBuffer.isOverflow()) {
                clearIngress(visitor);
                NettyHttpUtils.sendHttp413(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
                return;
            }
            if (complete == null) {
                break;
            }
            if (!complete.isReadable()) {
                ReferenceCountUtil.release(complete);
                break;
            }
            metricsCollector.recordInbound(config.getProxyId(), config.getAgentType(), complete.readableBytes());
            fileShareHttpHandler.handle(ctx, complete, ext);
            if (!state.frameBuffer.hasBuffered()) {
                visitor.attr(INGRESS).set(null);
                break;
            }
        }
    }

    private void feedStreaming(Channel visitor, IngressState state, ProxyConfig config, ByteBuf incoming) {
        int bytes = incoming.readableBytes();
        if (bytes > 0) {
            metricsCollector.recordInbound(config.getProxyId(), config.getAgentType(), bytes);
        }
        FileShareMultipartUploader.FeedResult result = state.uploader.feed(incoming);
        switch (result) {
            case NEED_MORE -> {
            }
            case SUCCESS -> {
                clearIngress(visitor);
                fileShareHttpHandler.writeJsonHttp(visitor, 200, Map.of("ok", true));
            }
            case BAD_REQUEST -> {
                String message = state.uploader.errorMessage();
                clearIngress(visitor);
                fileShareHttpHandler.writeJsonHttp(visitor, 400, Map.of("message", message));
            }
            case ERROR -> {
                String message = state.uploader.errorMessage();
                clearIngress(visitor);
                int status = message != null && message.contains("超时") ? 504 : 503;
                fileShareHttpHandler.writeJsonHttp(visitor, status, Map.of("message", message));
            }
        }
    }

    private void reject413(Channel visitor) {
        clearIngress(visitor);
        NettyHttpUtils.sendHttp413(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
    }

    private void clearIngress(Channel visitor) {
        IngressState state = visitor.attr(INGRESS).getAndSet(null);
        if (state != null) {
            state.discard(visitor);
        }
    }

    private static long resolveMaxUploadBytes(ProxyConfig config) {
        long max = FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE;
        if (config.hasFileShareLimits() && config.getFileShareLimits().getMaxUploadSize() != null) {
            max = config.getFileShareLimits().getMaxUploadSize();
        }
        return Math.max(MAX_HEADER_BYTES, max);
    }

    private static String stripQuery(String path) {
        if (path == null) {
            return "/";
        }
        int idx = path.indexOf('?');
        return idx >= 0 ? path.substring(0, idx) : path;
    }

    private static ParsedRequestLine parseRequestHeaders(ByteBuf buf, int headerBytes) {
        String headers = buf.toString(buf.readerIndex(), headerBytes, CharsetUtil.US_ASCII);
        String[] lines = headers.split("\r\n");
        if (lines.length == 0) {
            return null;
        }
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) {
            return null;
        }
        String method = requestLine[0];
        String path = requestLine[1];
        long contentLength = 0;
        String contentType = null;
        String cookie = null;
        boolean hasContentLength = false;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            switch (name) {
                case "content-length" -> {
                    try {
                        contentLength = Long.parseLong(value);
                        if (contentLength < 0) {
                            return null;
                        }
                        hasContentLength = true;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                case "content-type" -> contentType = value;
                case "cookie" -> cookie = value;
                default -> {
                }
            }
        }
        if (!hasContentLength && "POST".equalsIgnoreCase(method)) {
            contentLength = 0;
        }
        return new ParsedRequestLine(method, path, contentLength, contentType, cookie);
    }

    private static int indexOf(ByteBuf buf, byte[] target) {
        int readable = buf.readableBytes();
        int base = buf.readerIndex();
        outer:
        for (int i = 0; i <= readable - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (buf.getByte(base + i + j) != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clearIngress(ctx.channel());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (isOutOfMemoryError(cause)) {
            logger.warn("文件共享请求内存不足，关闭连接");
            clearIngress(ctx.channel());
            ChannelUtils.closeOnFlush(ctx.channel());
            return;
        }
        ctx.fireExceptionCaught(cause);
    }

    private static boolean isOutOfMemoryError(Throwable cause) {
        while (cause != null) {
            if (cause instanceof OutOfMemoryError) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private enum Mode {
        READING_HEADERS,
        BUFFERED,
        STREAMING
    }

    private static final class IngressState {
        Mode mode = Mode.READING_HEADERS;
        ByteBuf headerBuf;
        FileShareHttpFrameBuffer frameBuffer;
        FileShareMultipartUploader uploader;
        ProxyConfig proxyConfig;

        void discard(Channel channel) {
            if (headerBuf != null) {
                headerBuf.release();
                headerBuf = null;
            }
            if (frameBuffer != null) {
                frameBuffer.discard(channel);
                frameBuffer = null;
            }
            if (uploader != null) {
                uploader.close();
                uploader = null;
            }
            proxyConfig = null;
        }
    }

    private record ParsedRequestLine(String method, String path, long contentLength,
                                     String contentType, String cookie) {
    }
}
