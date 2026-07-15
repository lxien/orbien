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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;

final class FileShareHttpFrameBuffer {

    static final int MAX_HEADER_BYTES = 64 * 1024;

    private final long maxRequestBytes;
    private ByteBuf buffer;
    private long requiredBytes = -1;
    private boolean overflow;

    FileShareHttpFrameBuffer(long maxRequestBytes) {
        this.maxRequestBytes = Math.max(MAX_HEADER_BYTES, maxRequestBytes);
    }

    boolean isOverflow() {
        return overflow;
    }

    ByteBuf feed(Channel channel, ByteBuf incoming) {
        if (overflow) {
            ReferenceCountUtil.release(incoming);
            return null;
        }
        if (incoming != null) {
            if (!incoming.isReadable()) {
                ReferenceCountUtil.release(incoming);
                if (buffer == null || !buffer.isReadable()) {
                    return null;
                }
            } else {
                if (buffer == null) {
                    buffer = Unpooled.buffer(Math.min(incoming.readableBytes(), 8192));
                }
                if ((long) buffer.readableBytes() + incoming.readableBytes() > maxRequestBytes) {
                    ReferenceCountUtil.release(incoming);
                    markOverflow(channel);
                    return null;
                }
                buffer.writeBytes(incoming);
                ReferenceCountUtil.release(incoming);
            }
        } else if (buffer == null || !buffer.isReadable()) {
            return null;
        }

        if (requiredBytes < 0) {
            int headerEndIndex = indexOf(buffer, CRLFCRLF);
            if (headerEndIndex < 0) {
                if (buffer.readableBytes() >= MAX_HEADER_BYTES) {
                    markOverflow(channel);
                }
                return null;
            }
            int headerBytes = headerEndIndex + 4;
            if (headerBytes > MAX_HEADER_BYTES) {
                markOverflow(channel);
                return null;
            }
            long contentLength = parseContentLength(buffer, headerBytes);
            if (contentLength < 0) {
                markOverflow(channel);
                return null;
            }
            requiredBytes = headerBytes + contentLength;
            if (requiredBytes > maxRequestBytes || requiredBytes > Integer.MAX_VALUE) {
                markOverflow(channel);
                return null;
            }
        }

        if (buffer.readableBytes() < requiredBytes) {
            return null;
        }

        int take = (int) requiredBytes;
        ByteBuf complete;
        if (buffer.readableBytes() == take) {
            complete = buffer;
            buffer = null;
        } else {
            complete = buffer.readRetainedSlice(take);
        }
        requiredBytes = -1;
        return complete;
    }

    boolean hasBuffered() {
        return !overflow && buffer != null && buffer.isReadable();
    }

    void discard(Channel channel) {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
        requiredBytes = -1;
    }

    private void markOverflow(Channel channel) {
        overflow = true;
        discard(channel);
    }

    private static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private static long parseContentLength(ByteBuf buf, int headerBytes) {
        String headers = buf.toString(buf.readerIndex(), headerBytes, CharsetUtil.US_ASCII);
        for (String line : headers.split("\r\n")) {
            if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                try {
                    long value = Long.parseLong(line.substring(15).trim());
                    return value < 0 ? -1 : value;
                } catch (NumberFormatException ignored) {
                    return -1;
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
