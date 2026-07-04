package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

public class HeaderInjectDecoder extends ByteToMessageDecoder {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HeaderInjectDecoder.class);

    private static final int MAX_HEADER_SIZE = 65536;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() == 0) {
            return;
        }
        in.markReaderIndex();
        int headerEndIndex = findHeaderEnd(in);
        if (headerEndIndex < 0) {
            in.resetReaderIndex();
            return;
        }
        if (headerEndIndex > MAX_HEADER_SIZE) {
            logger.debug("[HTTP] HTTP请求头{}字节超过限制{}字节，关闭流", headerEndIndex, MAX_HEADER_SIZE);
            ChannelUtils.closeOnFlush(ctx.channel());
            return;
        }
        in.markReaderIndex();
        byte[] headerBytes = new byte[headerEndIndex];
        in.getBytes(in.readerIndex(), headerBytes);
        String headerContent = new String(headerBytes, CharsetUtil.UTF_8);

        if (!isHttpRequest(headerContent)) {
            in.resetReaderIndex();
            ctx.pipeline().remove(this);
            return;
        }

        Channel visitor = ctx.channel();
        String visitorIp = getVisitorIp(visitor);
        if (visitorIp == null || visitorIp.isEmpty()) {
            logger.warn("[HTTP] 客户端IP为空，跳过X-Forwarded-For注入");
            in.resetReaderIndex();
            return;
        }

        // 消费header字节
        in.skipBytes(headerEndIndex);

        // 注入X-Forwarded-For头
        String injectedHeader = injectXForwardedFor(headerContent, visitorIp);
        byte[] injectedHeaderBytes = injectedHeader.getBytes(CharsetUtil.UTF_8);

        // 构建新的复合缓冲区
        ByteBuf headerBuf = ctx.alloc().buffer(injectedHeaderBytes.length);
        headerBuf.writeBytes(injectedHeaderBytes);

        // 读取剩余body数据（不消费，保留给下一个handler）
        ByteBuf bodyBuf = in.readRetainedSlice(in.readableBytes());

        CompositeByteBuf compositeBuf = ctx.alloc().compositeBuffer(2);
        compositeBuf.addComponents(true, headerBuf, bodyBuf);
        out.add(compositeBuf);

        logger.debug("[HTTP] X-Forwarded-For注入完成，客户端IP={}", visitorIp);
    }
    protected String getVisitorIp(Channel visitor) {
        if (visitor.remoteAddress() instanceof InetSocketAddress addr) {
            return addr.getAddress().getHostAddress();
        }
        return visitor.remoteAddress().toString();
    }
    /**
     * 查找HTTP header的结束位置（\r\n\r\n）
     * 返回header结束位置（包含\r\n\r\n），未找到返回-1
     */
    private int findHeaderEnd(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        for (int i = 0; i < readableBytes - 3; i++) {
            if (buf.getByte(buf.readerIndex() + i) == '\r'
                    && buf.getByte(buf.readerIndex() + i + 1) == '\n'
                    && buf.getByte(buf.readerIndex() + i + 2) == '\r'
                    && buf.getByte(buf.readerIndex() + i + 3) == '\n') {
                return i + 4; // 包含\r\n\r\n
            }
        }
        return -1;
    }

    /**
     * 判断是否为HTTP请求
     */
    private boolean isHttpRequest(String content) {
        return content.startsWith("GET ") || content.startsWith("POST ")
                || content.startsWith("PUT ") || content.startsWith("DELETE ")
                || content.startsWith("HEAD ") || content.startsWith("OPTIONS ")
                || content.startsWith("PATCH ") || content.startsWith("CONNECT ")
                || content.startsWith("TRACE ");
    }

    /**
     * 注入或更新X-Forwarded-For头
     */
    private String injectXForwardedFor(String header, String clientIp) {
        StringBuilder result = new StringBuilder();
        String[] lines = header.split("\r\n");
        boolean xffFound = false;

        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            String lowerLine = line.toLowerCase();
            if (lowerLine.startsWith("x-forwarded-for:")) {
                String existingValue = line.substring(16).trim();
                result.append("X-Forwarded-For: ").append(existingValue).append(", ").append(clientIp).append("\r\n");
                xffFound = true;
            } else {
                result.append(line).append("\r\n");
            }
        }

        if (!xffFound) {
            result.append("X-Forwarded-For: ").append(clientIp).append("\r\n");
        }

        result.append("\r\n");
        return result.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("[HTTP] HeaderInjectDecoder异常", cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
