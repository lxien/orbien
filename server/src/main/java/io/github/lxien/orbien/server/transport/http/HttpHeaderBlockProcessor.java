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
package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.http.HeaderRewriteSupport;
import io.github.lxien.orbien.server.inspector.HttpMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP/1.1 头块改写与 body 边界跟踪（Content-Length / chunked 尾）
 */
public final class HttpHeaderBlockProcessor {
    public enum Mode {
        READ_HEADER,
        SKIP_BODY,
        PASSTHROUGH
    }

    private Mode mode = Mode.READ_HEADER;
    private long remainingBody;
    private boolean chunked;
    private final byte[] chunkedTail = new byte[5];
    private int chunkedTailLen;

    public Mode mode() {
        return mode;
    }

    public void setPassthrough() {
        mode = Mode.PASSTHROUGH;
    }

    /**
     * 若头块完整则消费头字节并返回改写后的头；不完整返回 null（不消费）
     */
    public byte[] tryRewriteHeader(ByteBuf in, boolean request, List<HeaderRewriteRule> rules, Map<String, String> vars) {
        if (mode != Mode.READ_HEADER) {
            return null;
        }
        int headerEnd = HttpMessageParser.findHeaderEnd(in);
        if (headerEnd < 0) {
            return null;
        }
        if (headerEnd > HeaderRewriteSupport.MAX_HEADER_BLOCK) {
            throw new IllegalStateException("HTTP header 超过限制");
        }

        byte[] raw = new byte[headerEnd];
        in.getBytes(in.readerIndex(), raw);
        String content = new String(raw, CharsetUtil.UTF_8);
        String[] lines = content.split("\r\n", -1);
        if (lines.length == 0) {
            mode = Mode.PASSTHROUGH;
            return null;
        }

        String startLine = lines[0];
        if (request) {
            if (!HttpMessageParser.isValidRequestLine(startLine)) {
                mode = Mode.PASSTHROUGH;
                return null;
            }
        } else if (!startLine.startsWith("HTTP/")) {
            mode = Mode.PASSTHROUGH;
            return null;
        }

        Map<String, String[]> headers = HeaderRewriteSupport.parseHeaderMap(lines, 1);
        HeaderRewriteSupport.apply(headers, rules, vars);
        byte[] out = HeaderRewriteSupport.serialize(startLine, headers).getBytes(CharsetUtil.UTF_8);

        in.skipBytes(headerEnd);
        armBodySkip(request, startLine, headers);
        return out;
    }

    /**
     * 根据报文推进 body 边界状态；不修改 buf readerIndex
     *
     * @return 本段可透传的 body 字节数
     */
    public int feedBody(ByteBuf buf) {
        if (mode == Mode.PASSTHROUGH) {
            return buf.readableBytes();
        }
        if (mode != Mode.SKIP_BODY) {
            return 0;
        }
        if (chunked) {
            int readable = buf.readableBytes();
            updateChunkedTail(buf, buf.readerIndex(), readable);
            if (isChunkedComplete()) {
                mode = Mode.READ_HEADER;
                chunkedTailLen = 0;
            }
            return readable;
        }
        int take = (int) Math.min(buf.readableBytes(), remainingBody);
        remainingBody -= take;
        if (remainingBody <= 0) {
            mode = Mode.READ_HEADER;
        }
        return take;
    }

    private void armBodySkip(boolean request, String startLine, Map<String, String[]> headers) {
        chunkedTailLen = 0;
        if (!request) {
            int status = parseStatus(startLine);
            if (status == 101 || status == 204 || status == 304 || (status >= 100 && status < 200)) {
                mode = Mode.READ_HEADER;
                return;
            }
        }
        String te = HeaderRewriteSupport.headerValue(headers, "Transfer-Encoding");
        if (te != null && te.toLowerCase(Locale.ROOT).contains("chunked")) {
            chunked = true;
            remainingBody = -1;
            mode = Mode.SKIP_BODY;
            return;
        }
        String cl = HeaderRewriteSupport.headerValue(headers, "Content-Length");
        if (cl != null) {
            try {
                remainingBody = Long.parseLong(cl.trim());
            } catch (NumberFormatException e) {
                mode = Mode.PASSTHROUGH;
                return;
            }
            chunked = false;
            mode = remainingBody > 0 ? Mode.SKIP_BODY : Mode.READ_HEADER;
            return;
        }
        if (request) {
            mode = Mode.READ_HEADER;
        } else {
            mode = Mode.PASSTHROUGH;
        }
    }

    private void updateChunkedTail(ByteBuf buf, int index, int length) {
        for (int i = index; i < index + length; i++) {
            byte b = buf.getByte(i);
            if (chunkedTailLen < chunkedTail.length) {
                chunkedTail[chunkedTailLen++] = b;
            } else {
                System.arraycopy(chunkedTail, 1, chunkedTail, 0, chunkedTail.length - 1);
                chunkedTail[chunkedTail.length - 1] = b;
            }
        }
    }

    private boolean isChunkedComplete() {
        return chunkedTailLen >= 5
                && chunkedTail[0] == '0'
                && chunkedTail[1] == '\r'
                && chunkedTail[2] == '\n'
                && chunkedTail[3] == '\r'
                && chunkedTail[4] == '\n';
    }

    private static int parseStatus(String statusLine) {
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
