package io.github.lxien.orbien.server.inspector;

import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 单条 HTTP 流的抓包状态。
 */
public class HttpStreamCapture {
    private static final int MAX_HEADER_SIZE = 65536;

    private final StreamContext context;
    private final InspectorProperties properties;
    private final Instant startedAt = Instant.now();
    private final StringBuilder requestBody = new StringBuilder();
    private final StringBuilder responseBody = new StringBuilder();

    private HttpMessageParser.ParsedRequest parsedRequest;
    private HttpMessageParser.ParsedResponse parsedResponse;
    private String rawRequestHeaders;
    private String rawResponseHeaders;
    private long requestBodySize;
    private long responseBodySize;
    private boolean requestBodyTruncated;
    private boolean responseBodyTruncated;
    private boolean finalized;
    private boolean responseStarted;
    private final byte[] chunkedTailBuf = new byte[5];
    private int chunkedTailLen;
    @Setter
    private Consumer<HttpCaptureRecord> completionHandler;

    public HttpStreamCapture(StreamContext context, InspectorProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    public void captureRequestFirstPacket(ByteBuf buf) {
        if (buf == null || !buf.isReadable() || parsedRequest != null) {
            return;
        }
        HttpMessageParser.ParsedRequest request = HttpMessageParser.parseRequest(buf, MAX_HEADER_SIZE);
        if (request == null) {
            return;
        }
        parsedRequest = request;
        rawRequestHeaders = request.rawHeaders();
        int headerEnd = HttpMessageParser.findHeaderEnd(buf);
        if (headerEnd > 0) {
            int bodyStart = buf.readerIndex() + headerEnd;
            int bodyLen = buf.readableBytes() - headerEnd;
            if (bodyLen > 0) {
                appendRequestBody(buf, bodyStart, bodyLen);
            }
        }
    }

    public void appendRequestBody(ByteBuf payload) {
        if (finalized || payload == null || !payload.isReadable()) {
            return;
        }
        appendRequestBody(payload, payload.readerIndex(), payload.readableBytes());
    }

    private void appendRequestBody(ByteBuf buf, int index, int length) {
        requestBodySize += length;
        if (requestBodyTruncated) {
            return;
        }
        int quota = properties.getMaxBodyBytes() - requestBody.length();
        if (quota <= 0) {
            requestBodyTruncated = true;
            return;
        }
        int toCopy = Math.min(length, quota);
        byte[] bytes = new byte[toCopy];
        buf.getBytes(index, bytes);
        requestBody.append(new String(bytes, StandardCharsets.UTF_8));
        if (toCopy < length) {
            requestBodyTruncated = true;
        }
    }

    public void appendResponseBody(ByteBuf payload) {
        if (finalized || payload == null || !payload.isReadable()) {
            return;
        }
        if (!responseStarted) {
            HttpMessageParser.ParsedResponse response = HttpMessageParser.parseResponse(payload, MAX_HEADER_SIZE);
            if (response != null) {
                parsedResponse = response;
                rawResponseHeaders = response.rawHeaders();
                responseStarted = true;
                int headerEnd = HttpMessageParser.findHeaderEnd(payload);
                if (headerEnd > 0) {
                    int bodyStart = payload.readerIndex() + headerEnd;
                    int bodyLen = payload.readableBytes() - headerEnd;
                    if (bodyLen > 0) {
                        appendResponseChunk(payload, bodyStart, bodyLen);
                    }
                }
                tryCompleteResponse();
                return;
            }
        }
        appendResponseChunk(payload, payload.readerIndex(), payload.readableBytes());
        tryCompleteResponse();
    }

    private void tryCompleteResponse() {
        if (finalized || !responseStarted || parsedResponse == null || !isHttpResponseComplete()) {
            return;
        }
        HttpCaptureRecord record = finalizeCapture();
        if (record != null && completionHandler != null) {
            completionHandler.accept(record);
        }
    }

    private boolean isHttpResponseComplete() {
        int status = parsedResponse.status();
        if (status == 101 || status == 204 || status == 304) {
            return true;
        }
        if (parsedRequest != null && "HEAD".equalsIgnoreCase(parsedRequest.method())) {
            return true;
        }

        Map<String, String> headers = parsedResponse.headers();
        String transferEncoding = headerValue(headers, "Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
            return isChunkedBodyComplete();
        }

        String contentLength = headerValue(headers, "Content-Length");
        if (contentLength != null) {
            try {
                long expected = Long.parseLong(contentLength.trim());
                return responseBodySize >= expected;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean isChunkedBodyComplete() {
        return chunkedTailLen >= 5
                && chunkedTailBuf[0] == '0'
                && chunkedTailBuf[1] == '\r'
                && chunkedTailBuf[2] == '\n'
                && chunkedTailBuf[3] == '\r'
                && chunkedTailBuf[4] == '\n';
    }

    private static String headerValue(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void appendResponseChunk(ByteBuf buf, int index, int length) {
        responseBodySize += length;
        updateChunkedTail(buf, index, length);
        if (responseBodyTruncated) {
            return;
        }
        int quota = properties.getMaxBodyBytes() - responseBody.length();
        if (quota <= 0) {
            responseBodyTruncated = true;
            return;
        }
        int toCopy = Math.min(length, quota);
        byte[] bytes = new byte[toCopy];
        buf.getBytes(index, bytes);
        responseBody.append(new String(bytes, StandardCharsets.UTF_8));
        if (toCopy < length) {
            responseBodyTruncated = true;
        }
    }

    private void updateChunkedTail(ByteBuf buf, int index, int length) {
        for (int i = index; i < index + length; i++) {
            byte b = buf.getByte(i);
            if (chunkedTailLen < chunkedTailBuf.length) {
                chunkedTailBuf[chunkedTailLen++] = b;
            } else {
                System.arraycopy(chunkedTailBuf, 1, chunkedTailBuf, 0, chunkedTailBuf.length - 1);
                chunkedTailBuf[chunkedTailBuf.length - 1] = b;
            }
        }
    }

    public HttpCaptureRecord finalizeCapture() {
        if (finalized) {
            return null;
        }
        finalized = true;
        long durationMs = Math.max(0, Instant.now().toEpochMilli() - startedAt.toEpochMilli());
        String method = parsedRequest != null ? parsedRequest.method() : "";
        String path = parsedRequest != null ? parsedRequest.path() : "";
        String host = parsedRequest != null ? nullToEmpty(parsedRequest.host()) : nullToEmpty(context.getVisitorDomain());
        if (host.isEmpty()) {
            host = nullToEmpty(context.getVisitorDomain());
        }
        int status = parsedResponse != null ? parsedResponse.status() : 0;
        String statusText = parsedResponse != null ? parsedResponse.statusText() : "";
        Map<String, String> requestHeaders = parsedRequest != null
                ? new LinkedHashMap<>(parsedRequest.headers())
                : Map.of();
        Map<String, String> responseHeaders = parsedResponse != null
                ? new LinkedHashMap<>(parsedResponse.headers())
                : Map.of();
        String scheme = resolveScheme();
        String rawRequest = buildRawRequest();
        String rawResponse = buildRawResponse();

        return HttpCaptureRecord.builder()
                .id("req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .proxyId(context.getProxyId())
                .streamId(context.getStreamId())
                .startedAt(startedAt)
                .durationMs(durationMs)
                .clientIp(nullToEmpty(context.getSourceAddress()))
                .host(host)
                .method(method)
                .path(path)
                .scheme(scheme)
                .status(status)
                .statusText(statusText)
                .requestHeaders(requestHeaders)
                .responseHeaders(responseHeaders)
                .requestBodySize(requestBodySize)
                .responseBodySize(responseBodySize)
                .requestBodyPreview(requestBody.isEmpty() ? null : requestBody.toString())
                .responseBodyPreview(responseBody.isEmpty() ? null : responseBody.toString())
                .requestBodyTruncated(requestBodyTruncated)
                .responseBodyTruncated(responseBodyTruncated)
                .rawRequest(rawRequest)
                .rawResponse(rawResponse)
                .build();
    }

    private String buildRawRequest() {
        StringBuilder sb = new StringBuilder();
        if (rawRequestHeaders != null) {
            sb.append(rawRequestHeaders);
        }
        if (!requestBody.isEmpty()) {
            sb.append(requestBody);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String buildRawResponse() {
        StringBuilder sb = new StringBuilder();
        if (rawResponseHeaders != null) {
            sb.append(rawResponseHeaders);
        }
        if (!responseBody.isEmpty()) {
            sb.append(responseBody);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String resolveScheme() {
        if (context.getProtocol() != null && context.getProtocol().isHttps()) {
            return "https";
        }
        Integer port = context.getListenerPort();
        if (port != null && port == 443) {
            return "https";
        }
        return "http";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
