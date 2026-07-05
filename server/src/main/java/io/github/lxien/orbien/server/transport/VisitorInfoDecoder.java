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

package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class VisitorInfoDecoder extends ByteToMessageDecoder {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(VisitorInfoDecoder.class);
    private boolean sniffing = true;

    public VisitorInfoDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Channel visitor = ctx.channel();
        if (!sniffing) {
            out.add(in.retain());
            return;
        }
        if (in.readableBytes() < 8) {
            return;
        }
        in.markReaderIndex();
        boolean isHttp = false;
        String domain = null;
        String basicAuth = null;
        try {
            int len = Math.min(in.readableBytes(), 8192);
            byte[] bytes = new byte[len];
            in.readBytes(bytes);
            String content = new String(bytes, CharsetUtil.UTF_8);

            if (isHttp(content)) {
                String host = parseHost(content);
                if (host != null) {
                    if (host.contains(":")) {
                        domain = host.split(":")[0];
                    } else {
                        domain = host;
                    }
                    basicAuth = parseAuthHeader(content);
                }
                isHttp = true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            in.resetReaderIndex();
            sniffing = false;
        }
        if (isHttp) {
            visitor.attr(AttributeKeys.PROTOCOL_TYPE).set(ProtocolType.HTTP);
            visitor.attr(AttributeKeys.VISIT_DOMAIN).set(domain);
            visitor.attr(AttributeKeys.BASIC_AUTH_HEADER).set(basicAuth);
        }
        // handler 移除前必须把 cumulation 交给下游，否则 ByteBuf 泄漏
        out.add(in.retain());
        ctx.pipeline().remove(this);
    }

    private String parseAuthHeader(String content) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.toLowerCase().startsWith("authorization:")) {
                int colonIndex = trimmedLine.indexOf(':');
                if (colonIndex != -1) {
                    return trimmedLine.substring(colonIndex + 1).trim();
                }
            }
        }
        return null;
    }

    private boolean isHttp(String content) {
        return content.startsWith("GET ") ||
                content.startsWith("POST ") ||
                content.startsWith("PUT ") ||
                content.startsWith("DELETE ") ||
                content.startsWith("HEAD ") ||
                content.startsWith("OPTIONS ") ||
                content.startsWith("PATCH ") ||
                content.startsWith("CONNECT ");
    }

    private String parseHost(String content) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.toLowerCase().startsWith("host:")) {
                int colonIndex = trimmedLine.indexOf(':');
                if (colonIndex != -1) {
                    return trimmedLine.substring(colonIndex + 1).trim();
                }
            }
        }
        return null;
    }

}
