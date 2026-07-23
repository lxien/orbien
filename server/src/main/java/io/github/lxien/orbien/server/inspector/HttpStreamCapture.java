package io.github.lxien.orbien.server.inspector;

import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 单条 HTTP 流的抓包状态，同一 TCP 连接上可连续捕获多条 HTTP 交换
 * <p>
 * 跨 TCP/TLS 分片重组请求/响应头；按 Content-Length / chunked 精确切分 body
 * 忽略中间 1xx 响应；在 keep-alive 下于响应完成时立即落库，不等连接关闭
 */
public class HttpStreamCapture {
    private static final int MAX_HEADER_SIZE = 65536;

    private final StreamContext context;
    private final InspectorProperties properties;
    private Instant exchangeStartedAt = Instant.now();
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
    private boolean streamFinalized;
    private boolean requestStarted;
    private boolean responseStarted;
    private boolean responseChunked;
    private long responseBodyRemaining = -1;
    private final ChunkedBodyDecoder chunkedDecoder = new ChunkedBodyDecoder();
    private final HeaderAccumulator requestHeaderBuf = new HeaderAccumulator();
    private final HeaderAccumulator responseHeaderBuf = new HeaderAccumulator();
    @Setter
    private Consumer<HttpCaptureRecord> completionHandler;

    public HttpStreamCapture(StreamContext context, InspectorProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    /**
     * 在隧道桥接就绪前预解析首包请求头，不采集 body
     */
    public void captureRequestFirstPacket(ByteBuf buf) {
        if (streamFinalized || buf == null || !buf.isReadable() || parsedRequest != null) {
            return;
        }
        HttpMessageParser.ParsedRequest request = HttpMessageParser.parseRequest(buf, MAX_HEADER_SIZE);
        if (request == null) {
            return;
        }
        parsedRequest = request;
        rawRequestHeaders = request.rawHeaders();
    }

    public void appendRequestBody(ByteBuf payload) {
        if (streamFinalized || payload == null || !payload.isReadable()) {
            return;
        }
        ByteBuf remaining = payload;
        while (remaining != null && remaining.isReadable() && !streamFinalized) {
            remaining = consumeRequest(remaining);
        }
    }

    private ByteBuf consumeRequest(ByteBuf payload) {
        if (!requestStarted) {
            if (parsedRequest != null && requestHeaderBuf.isEmpty()) {
                requestStarted = true;
                return appendRequestBodyAfterHeaders(payload);
            }
            return beginOrAccumulateRequest(payload);
        }

        HttpMessageParser.ParsedRequest nextRequest = tryParseRequestAtStart(payload);
        if (nextRequest != null) {
            emitCurrentExchange();
            return beginRequest(payload, nextRequest);
        }
        appendRequestChunk(payload, payload.readerIndex(), payload.readableBytes());
        return null;
    }

    private ByteBuf beginOrAccumulateRequest(ByteBuf payload) {
        if (requestHeaderBuf.isEmpty()) {
            HttpMessageParser.ParsedRequest request = HttpMessageParser.parseRequest(payload, MAX_HEADER_SIZE);
            if (request != null) {
                return beginRequest(payload, request);
            }
        }
        requestHeaderBuf.append(payload);
        if (requestHeaderBuf.size() > MAX_HEADER_SIZE) {
            requestHeaderBuf.clear();
            return null;
        }
        int headerEnd = requestHeaderBuf.findHeaderEnd();
        if (headerEnd <= 0) {
            return null;
        }
        ByteBuf assembled = requestHeaderBuf.toByteBuf();
        try {
            HttpMessageParser.ParsedRequest request = HttpMessageParser.parseRequest(assembled, MAX_HEADER_SIZE);
            if (request == null) {
                requestHeaderBuf.clear();
                return null;
            }
            beginRequestFromAssembled(request, assembled, headerEnd);
            return null;
        } finally {
            requestHeaderBuf.clear();
        }
    }

    private ByteBuf beginRequest(ByteBuf payload, HttpMessageParser.ParsedRequest request) {
        parsedRequest = request;
        rawRequestHeaders = request.rawHeaders();
        requestStarted = true;
        return appendRequestBodyAfterHeaders(payload);
    }

    private void beginRequestFromAssembled(HttpMessageParser.ParsedRequest request, ByteBuf assembled, int headerEnd) {
        parsedRequest = request;
        rawRequestHeaders = request.rawHeaders();
        requestStarted = true;
        int bodyLen = assembled.readableBytes() - headerEnd;
        if (bodyLen > 0) {
            appendRequestChunk(assembled, assembled.readerIndex() + headerEnd, bodyLen);
        }
    }

    private ByteBuf appendRequestBodyAfterHeaders(ByteBuf payload) {
        int headerEnd = HttpMessageParser.findHeaderEnd(payload);
        if (headerEnd <= 0) {
            return null;
        }
        int bodyStart = payload.readerIndex() + headerEnd;
        int bodyLen = payload.readableBytes() - headerEnd;
        if (bodyLen > 0) {
            appendRequestChunk(payload, bodyStart, bodyLen);
        }
        return null;
    }

    private HttpMessageParser.ParsedRequest tryParseRequestAtStart(ByteBuf payload) {
        if (!payload.isReadable()) {
            return null;
        }
        return HttpMessageParser.parseRequest(payload, MAX_HEADER_SIZE);
    }

    private void appendRequestChunk(ByteBuf buf, int index, int length) {
        if (length <= 0) {
            return;
        }
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
        if (streamFinalized || payload == null || !payload.isReadable()) {
            return;
        }
        ByteBuf remaining = payload;
        while (remaining != null && remaining.isReadable() && !streamFinalized) {
            remaining = consumeResponse(remaining);
        }
    }

    private ByteBuf consumeResponse(ByteBuf payload) {
        if (!responseStarted) {
            return beginOrAccumulateResponse(payload);
        }
        return appendResponseBodyBytes(payload);
    }

    private ByteBuf beginOrAccumulateResponse(ByteBuf payload) {
        if (responseHeaderBuf.isEmpty()) {
            HttpMessageParser.ParsedResponse response = HttpMessageParser.parseResponse(payload, MAX_HEADER_SIZE);
            if (response != null) {
                int headerEnd = HttpMessageParser.findHeaderEnd(payload);
                return onResponseHeaders(response, payload, headerEnd);
            }
        }
        responseHeaderBuf.append(payload);
        if (responseHeaderBuf.size() > MAX_HEADER_SIZE) {
            responseHeaderBuf.clear();
            return null;
        }
        int headerEnd = responseHeaderBuf.findHeaderEnd();
        if (headerEnd <= 0) {
            return null;
        }
        ByteBuf assembled = responseHeaderBuf.toByteBuf();
        try {
            HttpMessageParser.ParsedResponse response = HttpMessageParser.parseResponse(assembled, MAX_HEADER_SIZE);
            if (response == null) {
                responseHeaderBuf.clear();
                return null;
            }
            return onResponseHeaders(response, assembled, headerEnd);
        } finally {
            responseHeaderBuf.clear();
        }
    }

    private ByteBuf onResponseHeaders(HttpMessageParser.ParsedResponse response, ByteBuf source, int headerEnd) {
        int status = response.status();
        if (status >= 100 && status < 200 && status != 101) {
            int bodyStart = source.readerIndex() + Math.max(headerEnd, 0);
            int leftover = source.readableBytes() - Math.max(headerEnd, 0);
            if (leftover <= 0) {
                return null;
            }
            return source.slice(bodyStart, leftover);
        }

        parsedResponse = response;
        rawResponseHeaders = response.rawHeaders();
        responseStarted = true;
        armResponseBodyTracking(response);

        int bodyOffset = Math.max(headerEnd, 0);
        int available = source.readableBytes() - bodyOffset;
        if (available <= 0) {
            if (isHttpResponseComplete()) {
                emitCurrentExchange();
            }
            return null;
        }
        ByteBuf body = source.slice(source.readerIndex() + bodyOffset, available);
        return appendResponseBodyBytes(body);
    }

    private void armResponseBodyTracking(HttpMessageParser.ParsedResponse response) {
        responseChunked = false;
        responseBodyRemaining = -1;
        chunkedDecoder.reset();

        int status = response.status();
        if (status == 101 || status == 204 || status == 304) {
            responseBodyRemaining = 0;
            return;
        }
        if (parsedRequest != null && "HEAD".equalsIgnoreCase(parsedRequest.method())) {
            responseBodyRemaining = 0;
            return;
        }

        Map<String, String> headers = response.headers();
        String transferEncoding = headerValue(headers, "Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
            responseChunked = true;
            responseBodyRemaining = -1;
            return;
        }
        String contentLength = headerValue(headers, "Content-Length");
        if (contentLength != null) {
            try {
                responseBodyRemaining = Math.max(0, Long.parseLong(contentLength.trim()));
            } catch (NumberFormatException ignored) {
                responseBodyRemaining = -1;
            }
        }
    }

    private ByteBuf appendResponseBodyBytes(ByteBuf payload) {
        int reader = payload.readerIndex();
        int available = payload.readableBytes();
        if (available <= 0) {
            return null;
        }

        if (responseChunked) {
            int consumed = chunkedDecoder.feed(payload, reader, available, this::appendDecodedResponseBody);
            if (chunkedDecoder.isDone()) {
                emitCurrentExchange();
                int leftover = available - consumed;
                if (leftover > 0) {
                    return payload.slice(reader + consumed, leftover);
                }
            }
            return null;
        }

        int take;
        if (responseBodyRemaining >= 0) {
            take = (int) Math.min(available, responseBodyRemaining);
        } else {
            take = available;
        }

        if (take > 0) {
            appendDecodedResponseBody(payload, reader, take);
            if (responseBodyRemaining > 0) {
                responseBodyRemaining -= take;
            }
        }

        if (isHttpResponseComplete()) {
            emitCurrentExchange();
            int leftover = available - take;
            if (leftover > 0) {
                return payload.slice(reader + take, leftover);
            }
            return null;
        }
        return null;
    }

    private void emitCurrentExchange() {
        if (parsedRequest == null) {
            resetForNextExchange();
            return;
        }
        HttpCaptureRecord record = buildRecord();
        if (record != null && completionHandler != null) {
            completionHandler.accept(record);
        }
        resetForNextExchange();
    }

    private void resetForNextExchange() {
        parsedRequest = null;
        parsedResponse = null;
        rawRequestHeaders = null;
        rawResponseHeaders = null;
        requestBody.setLength(0);
        responseBody.setLength(0);
        requestBodySize = 0;
        responseBodySize = 0;
        requestBodyTruncated = false;
        responseBodyTruncated = false;
        requestStarted = false;
        responseStarted = false;
        responseChunked = false;
        responseBodyRemaining = -1;
        chunkedDecoder.reset();
        requestHeaderBuf.clear();
        responseHeaderBuf.clear();
        exchangeStartedAt = Instant.now();
    }

    private boolean isHttpResponseComplete() {
        if (!responseStarted || parsedResponse == null) {
            return false;
        }
        int status = parsedResponse.status();
        if (status == 101 || status == 204 || status == 304) {
            return true;
        }
        if (parsedRequest != null && "HEAD".equalsIgnoreCase(parsedRequest.method())) {
            return true;
        }
        if (responseChunked) {
            return chunkedDecoder.isDone();
        }
        if (responseBodyRemaining >= 0) {
            return responseBodyRemaining == 0;
        }
        return false;
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

    /**
     * 仅写入解码后的 body 字节（chunked 已去掉长度行）
     */
    private void appendDecodedResponseBody(ByteBuf buf, int index, int length) {
        if (length <= 0) {
            return;
        }
        responseBodySize += length;
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

    public HttpCaptureRecord finalizeCapture() {
        if (streamFinalized) {
            return null;
        }
        streamFinalized = true;
        if (parsedRequest == null && !responseStarted) {
            return null;
        }
        return buildRecord();
    }

    private HttpCaptureRecord buildRecord() {
        long durationMs = Math.max(0, Instant.now().toEpochMilli() - exchangeStartedAt.toEpochMilli());
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
                .startedAt(exchangeStartedAt)
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
                .replay(context.isReplay())
                .sourceRecordId(context.isReplay() ? context.getReplaySourceRecordId() : null)
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
        Channel visitor = context.getVisitor();
        if (visitor != null && visitor.pipeline().get(SslHandler.class) != null) {
            return "https";
        }
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

    @FunctionalInterface
    private interface DecodedBodySink {
        void accept(ByteBuf buf, int index, int length);
    }

    private static final class ChunkedBodyDecoder {
        private enum State {
            SIZE, DATA, CRLF, TRAILER, DONE
        }

        private State state = State.SIZE;
        private final StringBuilder sizeHex = new StringBuilder(16);
        private long dataRemaining;
        private int crlfNeed;
        private int trailerEndMatch;

        void reset() {
            state = State.SIZE;
            sizeHex.setLength(0);
            dataRemaining = 0;
            crlfNeed = 0;
            trailerEndMatch = 0;
        }

        boolean isDone() {
            return state == State.DONE;
        }

        /**
         * @return 已消费的字节数
         */
        int feed(ByteBuf buf, int index, int length, DecodedBodySink sink) {
            int i = index;
            int end = index + length;
            while (i < end && state != State.DONE) {
                switch (state) {
                    case SIZE -> i = feedSize(buf, i, end);
                    case DATA -> i = feedData(buf, i, end, sink);
                    case CRLF -> i = feedCrlf(buf, i, end);
                    case TRAILER -> i = feedTrailer(buf, i, end);
                    default -> {
                        return i - index;
                    }
                }
            }
            return i - index;
        }

        private int feedSize(ByteBuf buf, int i, int end) {
            while (i < end) {
                byte b = buf.getByte(i++);
                if (b == '\n') {
                    String line = sizeHex.toString().trim();
                    sizeHex.setLength(0);
                    int semi = line.indexOf(';');
                    if (semi >= 0) {
                        line = line.substring(0, semi).trim();
                    }
                    long size;
                    try {
                        size = line.isEmpty() ? 0L : Long.parseLong(line, 16);
                    } catch (NumberFormatException ex) {
                        state = State.DONE;
                        return i;
                    }
                    if (size <= 0) {
                        state = State.TRAILER;
                        trailerEndMatch = 0;
                    } else {
                        dataRemaining = size;
                        state = State.DATA;
                    }
                    return i;
                }
                if (b != '\r' && sizeHex.length() < 64) {
                    sizeHex.append((char) (b & 0xFF));
                }
            }
            return i;
        }

        private int feedData(ByteBuf buf, int i, int end, DecodedBodySink sink) {
            int available = end - i;
            int take = (int) Math.min(available, dataRemaining);
            if (take > 0) {
                sink.accept(buf, i, take);
                i += take;
                dataRemaining -= take;
            }
            if (dataRemaining == 0) {
                state = State.CRLF;
                crlfNeed = 2;
            }
            return i;
        }

        private int feedCrlf(ByteBuf buf, int i, int end) {
            while (i < end && crlfNeed > 0) {
                byte b = buf.getByte(i++);
                if (crlfNeed == 2) {
                    if (b == '\r') {
                        crlfNeed = 1;
                    } else if (b == '\n') {
                        crlfNeed = 0;
                    }
                } else if (b == '\n') {
                    crlfNeed = 0;
                }
            }
            if (crlfNeed == 0) {
                state = State.SIZE;
            }
            return i;
        }

        private int feedTrailer(ByteBuf buf, int i, int end) {
            while (i < end) {
                byte b = buf.getByte(i++);
                if (b == '\r' && trailerEndMatch % 2 == 0) {
                    trailerEndMatch++;
                } else if (b == '\n' && trailerEndMatch % 2 == 1) {
                    trailerEndMatch++;
                    if (trailerEndMatch >= 4) {
                        state = State.DONE;
                        return i;
                    }
                } else if (b == '\r') {
                    trailerEndMatch = 1;
                } else {
                    trailerEndMatch = 0;
                }
            }
            return i;
        }
    }

    /**
     * 跨分片累积 HTTP 头块，避免 TLS/TCP 半包导致永远解析不出响应头
     */
    private static final class HeaderAccumulator {
        private byte[] buf = new byte[256];
        private int len;

        void append(ByteBuf src) {
            int n = src.readableBytes();
            if (n <= 0) {
                return;
            }
            ensureCapacity(len + n);
            src.getBytes(src.readerIndex(), buf, len, n);
            len += n;
        }

        int findHeaderEnd() {
            for (int i = 0; i < len - 3; i++) {
                if (buf[i] == '\r'
                        && buf[i + 1] == '\n'
                        && buf[i + 2] == '\r'
                        && buf[i + 3] == '\n') {
                    return i + 4;
                }
            }
            return -1;
        }

        ByteBuf toByteBuf() {
            return Unpooled.wrappedBuffer(buf, 0, len);
        }

        int size() {
            return len;
        }

        boolean isEmpty() {
            return len == 0;
        }

        void clear() {
            len = 0;
        }

        private void ensureCapacity(int needed) {
            if (needed <= buf.length) {
                return;
            }
            int cap = buf.length;
            while (cap < needed) {
                cap <<= 1;
            }
            buf = Arrays.copyOf(buf, cap);
        }
    }
}
