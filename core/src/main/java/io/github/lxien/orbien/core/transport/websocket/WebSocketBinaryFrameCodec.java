package io.github.lxien.orbien.core.transport.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * WebSocket 二进制帧与 TMSP/Snappy 字节流之间的桥接编解码器。
 * Netty WebSocket 协议 handler 在握手后向下游传递 {@link WebSocketFrame}，
 * 而 TMSP 栈期望 {@link ByteBuf}，缺少此转换会导致服务端立即关闭连接。
 */
public final class WebSocketBinaryFrameCodec extends CombinedChannelDuplexHandler<
        WebSocketBinaryFrameCodec.Decoder, WebSocketBinaryFrameCodec.Encoder> {

    public WebSocketBinaryFrameCodec() {
        super(new Decoder(), new Encoder());
    }

    static final class Decoder extends MessageToMessageDecoder<WebSocketFrame> {
        private static final InternalLogger logger = InternalLoggerFactory.getInstance(Decoder.class);

        @Override
        protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) {
            if (frame instanceof BinaryWebSocketFrame) {
                out.add(frame.content().retain());
                return;
            }
            if (!frame.isFinalFragment()) {
                logger.warn("忽略非 final WebSocket 分片帧");
                return;
            }
            logger.warn("忽略非二进制 WebSocket 帧: {}", frame.getClass().getSimpleName());
        }
    }

    static final class Encoder extends MessageToMessageEncoder<ByteBuf> {
        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
            out.add(new BinaryWebSocketFrame(msg.retain()));
        }
    }
}
