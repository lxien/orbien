package io.github.lxien.orbien.server.utils;

import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.CharsetUtil;


import io.netty.channel.*;

/**
 * HTTP 工具类
 */
public class NettyHttpUtils {

    private static ChannelFuture writeAndFlush(Channel channel, ByteBuf buf) {
        return channel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                buf.release();
            }
        });
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
        int contentLength = content.length();

        String response = String.format("""
                HTTP/1.1 200 OK\r
                Content-Type: text/plain\r
                Content-Length: %d\r
                \r
                %s""", contentLength, content);

        return writeAndFlush(channel, buildResponse(channel, response));
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

    public static ChannelFuture sendHttpResponse(
            Channel channel,
            int statusCode,
            String statusMessage,
            String contentType,
            String content
    ) {
        int contentLength = content.length();

        String response = String.format("""
                HTTP/1.1 %d %s\r
                Content-Type: %s\r
                Content-Length: %d\r
                \r
                %s""", statusCode, statusMessage, contentType, contentLength, content);

        return writeAndFlush(channel, buildResponse(channel, response));
    }
}
