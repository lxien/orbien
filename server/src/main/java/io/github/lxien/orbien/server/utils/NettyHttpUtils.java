package io.github.lxien.orbien.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 工具类
 */
public class NettyHttpUtils {

    private static ChannelFuture writeAndFlush(Channel channel, ByteBuf buf) {
        return channel.writeAndFlush(buf);
    }

    private static ByteBuf buildResponse(Channel channel, String response) {
        ByteBuf buf = channel.alloc().buffer(response.length());
        buf.writeCharSequence(response, CharsetUtil.UTF_8);
        return buf;
    }

    public static ChannelFuture sendHttp403(Channel channel) {
        String response = """
                HTTP/1.1 403 Forbidden\r
                Content-Type: text/plain\r
                Content-Length: 15\r
                \r
                Access Denied""";
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttpTooManyRequests(Channel channel) {
        String response = """
                HTTP/1.1 429 Too Many Requests\r
                Content-Length: 0\r
                Retry-After: 1\r
                \r
                """;
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    /**
     * 401 Basic Auth
     */
    public static ChannelFuture sendBasicAuth(Channel channel) {
        String response = """
                HTTP/1.1 401 Unauthorized\r
                WWW-Authenticate: Basic realm="Authentication Required"\r
                Content-Type: text/html; charset=UTF-8\r
                Content-Length: 0\r
                Connection: close\r
                \r
                """;
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttp200(Channel channel, String content) {
        return sendHttpResponse(channel, 200, "OK", "text/plain; charset=UTF-8", content);
    }

    public static ChannelFuture sendHttp404(Channel channel) {
        String response = """
                HTTP/1.1 404 Not Found\r
                Content-Type: text/plain\r
                Content-Length: 13\r
                \r
                Not Found""";
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttp502(Channel channel) {
        return sendHttpResponse(channel, 502, "Bad Gateway", "text/plain; charset=UTF-8", "Bad Gateway");
    }

    public static ChannelFuture sendHttp503(Channel channel) {
        return sendHttpResponse(channel, 503, "Service Unavailable", "text/plain; charset=UTF-8", "Service Unavailable");
    }

    public static ChannelFuture sendHttp504(Channel channel) {
        return sendHttpResponse(channel, 504, "Gateway Timeout", "text/plain; charset=UTF-8", "Gateway Timeout");
    }

    public static ChannelFuture sendHttp500(Channel channel) {
        String response = """
                HTTP/1.1 500 Internal Server Error\r
                Content-Type: text/plain\r
                Content-Length: 21\r
                \r
                Internal Server Error""";
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttp400(Channel channel) {
        String response = """
                HTTP/1.1 400 Bad Request\r
                Content-Type: text/plain\r
                Content-Length: 11\r
                \r
                Bad Request""";
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttp413(Channel channel) {
        String response = """
                HTTP/1.1 413 Content Too Large\r
                Content-Type: text/plain\r
                Content-Length: 17\r
                Connection: close\r
                \r
                Content Too Large""";
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    /**
     * 发送 HTTP 重定向（默认 308 Permanent Redirect）。
     */
    public static ChannelFuture sendRedirect(Channel channel, int statusCode, String location) {
        String reasonPhrase = statusCode == 308 ? "Permanent Redirect"
                : statusCode == 301 ? "Moved Permanently"
                : "Redirect";
        String response = String.format("""
                HTTP/1.1 %d %s\r
                Location: %s\r
                Content-Length: 0\r
                Connection: close\r
                \r
                """, statusCode, reasonPhrase, location);
        return writeAndFlush(channel, buildResponse(channel, response));
    }

    public static ChannelFuture sendHttpResponse(
            Channel channel,
            int statusCode,
            String statusMessage,
            String contentType,
            String content
    ) {
        byte[] bodyBytes = content.getBytes(StandardCharsets.UTF_8);
        return sendHttpBinaryResponse(channel, statusCode, statusMessage, contentType, bodyBytes);
    }

    public static ChannelFuture sendHttpBinaryResponse(
            Channel channel,
            int statusCode,
            String statusMessage,
            String contentType,
            byte[] bodyBytes
    ) {
        byte[] safeBody = bodyBytes == null ? new byte[0] : bodyBytes;
        String headers = String.format("""
                HTTP/1.1 %d %s\r
                Content-Type: %s\r
                Content-Length: %d\r
                Connection: close\r
                \r
                """, statusCode, statusMessage, contentType, safeBody.length);
        ByteBuf buf = channel.alloc().buffer(headers.length() + safeBody.length);
        buf.writeCharSequence(headers, CharsetUtil.UTF_8);
        buf.writeBytes(safeBody);
        return writeAndFlush(channel, buf);
    }
}
