package io.github.lxien.orbien.server.transport.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/**
 * 从 HTTP 请求首包解析请求行（method / request-uri / version）。
 */
public final class HttpFirstLineParser {

    public record HttpFirstLine(String method, String requestUri, String httpVersion) {
    }

    private HttpFirstLineParser() {
    }

    public static HttpFirstLine parse(ByteBuf in) {
        if (in == null || in.readableBytes() < 4) {
            return null;
        }
        int scanLength = Math.min(in.readableBytes(), 8192);
        int start = in.readerIndex();
        int lineEnd = -1;
        for (int i = start; i < start + scanLength - 1; i++) {
            if (in.getByte(i) == '\r' && in.getByte(i + 1) == '\n') {
                lineEnd = i;
                break;
            }
        }
        if (lineEnd < 0) {
            return null;
        }
        int lineLength = lineEnd - start;
        if (lineLength <= 0) {
            return null;
        }
        String line = in.toString(start, lineLength, CharsetUtil.US_ASCII);
        int firstSpace = line.indexOf(' ');
        if (firstSpace <= 0) {
            return null;
        }
        int secondSpace = line.indexOf(' ', firstSpace + 1);
        if (secondSpace <= firstSpace) {
            return null;
        }
        String method = line.substring(0, firstSpace);
        String requestUri = line.substring(firstSpace + 1, secondSpace);
        String version = line.substring(secondSpace + 1);
        return new HttpFirstLine(method, requestUri, version);
    }
}
