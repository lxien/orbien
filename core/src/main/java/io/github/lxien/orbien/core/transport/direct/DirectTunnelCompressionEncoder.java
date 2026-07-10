package io.github.lxien.orbien.core.transport.direct;

import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 独立隧道出站压缩：将明文 {@link ByteBuf} 编码为分块字节流。
 */
public class DirectTunnelCompressionEncoder extends MessageToByteEncoder<ByteBuf> {

    private final CompressionType algorithm;

    public DirectTunnelCompressionEncoder(CompressionType algorithm) {
        this.algorithm = algorithm != null ? algorithm : CompressionType.NONE;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        if (!msg.isReadable()) {
            return;
        }
        if (!algorithm.isCompressed()) {
            out.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
            return;
        }
        ByteBuf framed = DirectTunnelChunkCodec.encode(ctx.channel(), msg, algorithm);
        try {
            out.writeBytes(framed);
        } finally {
            framed.release();
        }
    }
}
