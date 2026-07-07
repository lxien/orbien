package io.github.lxien.orbien.server.inspector;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 报文嗅探解析，不消费待转发的 {@link ByteBuf}。
 */
public final class HttpMessageParser {
    private HttpMessageParser() {
    }

    public static int findHeaderEnd(ByteBuf buf) {
        int readable = buf.readableBytes();
        int base = buf.readerIndex();
        for (int i = 0; i < readable - 3; i++) {
            if (buf.getByte(base + i) == '\r'
                    && buf.getByte(base + i + 1) == '\n'
                    && buf.getByte(base + i + 2) == '\r'
                    && buf.getByte(base + i + 3) == '\n') {
                return i + 4;
            }
        }
        return -1;
    }

    public static ParsedRequest parseRequest(ByteBuf buf, int maxHeaderSize) {
        if (buf == null || !buf.isReadable()) {
            return null;
        }
        int headerEnd = findHeaderEnd(buf);
        if (headerEnd <= 0 || headerEnd > maxHeaderSize) {
            return null;
        }
        byte[] headerBytes = new byte[headerEnd];
        buf.getBytes(buf.readerIndex(), headerBytes);
        return parseRequestHeaders(new String(headerBytes, CharsetUtil.UTF_8));
    }

    public static ParsedRequest parseRequestHeaders(String headerContent) {
        if (headerContent == null || headerContent.isBlank()) {
            return null;
        }
        String[] lines = headerContent.split("\r\n");
        if (lines.length == 0) {
            return null;
        }
        String requestLine = lines[0];
        int firstSpace = requestLine.indexOf(' ');
        int secondSpace = requestLine.indexOf(' ', firstSpace + 1);
        if (firstSpace <= 0 || secondSpace <= firstSpace) {
            return null;
        }
        String method = requestLine.substring(0, firstSpace);
        String path = requestLine.substring(firstSpace + 1, secondSpace);
        Map<String, String> headers = parseHeaders(lines, 1);
        return new ParsedRequest(method, path, headers, headerContent);
    }

    public static ParsedResponse parseResponse(ByteBuf buf, int maxHeaderSize) {
        if (buf == null || !buf.isReadable()) {
            return null;
        }
        int headerEnd = findHeaderEnd(buf);
        if (headerEnd <= 0 || headerEnd > maxHeaderSize) {
            return null;
        }
        byte[] headerBytes = new byte[headerEnd];
        buf.getBytes(buf.readerIndex(), headerBytes);
        return parseResponseHeaders(new String(headerBytes, CharsetUtil.UTF_8));
    }

    public static ParsedResponse parseResponseHeaders(String headerContent) {
        if (headerContent == null || headerContent.isBlank()) {
            return null;
        }
        String[] lines = headerContent.split("\r\n");
        if (lines.length == 0 || !lines[0].startsWith("HTTP/")) {
            return null;
        }
        String statusLine = lines[0];
        String[] parts = statusLine.split(" ", 3);
        int status = 0;
        String statusText = "";
        if (parts.length >= 2) {
            try {
                status = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                status = 0;
            }
            if (parts.length >= 3) {
                statusText = parts[2];
            }
        }
        Map<String, String> headers = parseHeaders(lines, 1);
        return new ParsedResponse(status, statusText, headers, headerContent);
    }

    private static Map<String, String> parseHeaders(String[] lines, int start) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            headers.put(name, value);
        }
        return headers;
    }

    public record ParsedRequest(String method, String path, Map<String, String> headers, String rawHeaders) {
        public String host() {
            String host = headers.get("Host");
            if (host == null) {
                host = headers.get("host");
            }
            return host;
        }
    }

    public record ParsedResponse(int status, String statusText, Map<String, String> headers, String rawHeaders) {
    }
}
