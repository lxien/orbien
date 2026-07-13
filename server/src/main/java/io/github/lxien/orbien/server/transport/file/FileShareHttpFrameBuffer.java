package io.github.lxien.orbien.server.transport.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;

/**
 * 聚合 HTTP 请求帧（支持大 body 分片到达，如 multipart 上传）
 */
final class FileShareHttpFrameBuffer {

    private ByteBuf buffer;
    /**
     * 完整 HTTP 报文长度（请求头 + body）
     */
    private int requiredBytes = -1;

    ByteBuf feed(Channel channel, ByteBuf incoming) {
        if (incoming != null) {
            if (!incoming.isReadable()) {
                ReferenceCountUtil.release(incoming);
                if (buffer == null || !buffer.isReadable()) {
                    return null;
                }
            } else {
                if (buffer == null) {
                    buffer = Unpooled.buffer(incoming.readableBytes());
                }
                buffer.writeBytes(incoming);
                ReferenceCountUtil.release(incoming);
            }
        } else if (buffer == null || !buffer.isReadable()) {
            return null;
        }

        if (requiredBytes < 0) {
            int headerEndIndex = indexOf(buffer, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            if (headerEndIndex < 0) {
                return null;
            }
            int headerBytes = headerEndIndex + 4;
            int contentLength = parseContentLength(buffer, headerBytes);
            requiredBytes = headerBytes + contentLength;
            if (requiredBytes <= 0) {
                discard(channel);
                return null;
            }
        }

        if (buffer.readableBytes() < requiredBytes) {
            return null;
        }

        ByteBuf complete;
        if (buffer.readableBytes() == requiredBytes) {
            complete = buffer;
            buffer = null;
        } else {
            complete = buffer.readRetainedSlice(requiredBytes);
        }
        requiredBytes = -1;
        return complete;
    }

    boolean hasBuffered() {
        return buffer != null && buffer.isReadable();
    }

    void discard(Channel channel) {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
        requiredBytes = -1;
    }

    private static int parseContentLength(ByteBuf buf, int headerBytes) {
        String headers = buf.toString(buf.readerIndex(), headerBytes, CharsetUtil.US_ASCII);
        for (String line : headers.split("\r\n")) {
            if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                try {
                    return Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
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
}
