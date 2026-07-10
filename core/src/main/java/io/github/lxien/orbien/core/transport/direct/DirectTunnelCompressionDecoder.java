package io.github.lxien.orbien.core.transport.direct;

import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * 独立隧道入站解压：将分块字节流还原为明文 {@link ByteBuf}。
 */
public class DirectTunnelCompressionDecoder extends ByteToMessageDecoder {

    private final CompressionType algorithm;

    public DirectTunnelCompressionDecoder(CompressionType algorithm) {
        this.algorithm = algorithm != null ? algorithm : CompressionType.NONE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!algorithm.isCompressed()) {
            if (in.isReadable()) {
                out.add(in.readRetainedSlice(in.readableBytes()));
            }
            return;
        }
        while (in.readableBytes() >= DirectTunnelChunkCodec.HEADER_LENGTH) {
            in.markReaderIndex();
            byte type = in.getByte(in.readerIndex());
            int bodyLen = in.getInt(in.readerIndex() + 1);
            try {
                DirectTunnelChunkCodec.validateBodyLength(bodyLen);
            } catch (CorruptedFrameException e) {
                in.resetReaderIndex();
                throw e;
            }
            if (in.readableBytes() < DirectTunnelChunkCodec.HEADER_LENGTH + bodyLen) {
                in.resetReaderIndex();
                return;
            }
            in.skipBytes(DirectTunnelChunkCodec.HEADER_LENGTH);
            ByteBuf body = in.readRetainedSlice(bodyLen);
            try {
                out.add(DirectTunnelChunkCodec.decodeBody(ctx.channel(), type, body));
            } finally {
                body.release();
            }
        }
    }
}
