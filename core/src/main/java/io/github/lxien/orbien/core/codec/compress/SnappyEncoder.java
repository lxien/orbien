package io.github.lxien.orbien.core.codec.compress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


public class SnappyEncoder extends SnappyFrameEncoder {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SnappyEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (logger.isDebugEnabled() && in.readableBytes() > MIN_COMPRESSIBLE_LENGTH) {
            int beforeCompressSize = in.readableBytes();
            int initialOutSize = out.writerIndex();

            super.encode(ctx, in, out);

            int afterCompressSize = out.writerIndex() - initialOutSize;
            if (beforeCompressSize > 0) {
                // 压缩率 = (1 - 压缩后大小 / 压缩前大小) * 100%
                double compressionRatio = (1 - (double) afterCompressSize / beforeCompressSize) * 100;
                logger.debug("压缩前: {} 字节, 压缩后: {} 字节, 压缩率: {}%",
                        beforeCompressSize,
                        afterCompressSize,
                        String.format("%.2f", compressionRatio));
            }
        } else {
            super.encode(ctx, in, out);
        }
    }
}