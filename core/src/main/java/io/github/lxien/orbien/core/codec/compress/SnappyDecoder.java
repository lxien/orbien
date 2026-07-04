package io.github.lxien.orbien.core.codec.compress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;


public class SnappyDecoder extends SnappyFrameDecoder {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SnappyDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (logger.isDebugEnabled()) {
            final int inSize = in.readableBytes();
            if (inSize < 4) {
                return;
            }
            final int chunkTypeVal = in.getUnsignedByte(in.readerIndex());
            if (chunkTypeVal != 0) {
                super.decode(ctx, in, out);
                return;
            }
            int beforeDecompressSize = in.readableBytes();
            int initialOutSize = out.size();

            super.decode(ctx, in, out);

            int afterDecompressSize = 0;
            if (out.size() > initialOutSize) {
                for (int i = initialOutSize; i < out.size(); i++) {
                    Object obj = out.get(i);
                    if (obj instanceof ByteBuf) {
                        afterDecompressSize += ((ByteBuf) obj).readableBytes();
                    }
                }
            }

            if (beforeDecompressSize > 0 && afterDecompressSize > 0) {
                // 压缩率 = (1 - 压缩前大小 / 解压后大小) * 100%
                double compressionRatio = (1 - (double) beforeDecompressSize / afterDecompressSize) * 100;
                logger.debug("解压前: {} 字节, 解压后: {} 字节, 压缩率: {}%",
                        beforeDecompressSize,
                        afterDecompressSize,
                        String.format("%.2f", compressionRatio));
            }
        } else {
            super.decode(ctx, in, out);
        }
    }
}
