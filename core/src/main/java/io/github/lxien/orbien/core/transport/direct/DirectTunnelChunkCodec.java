package io.github.lxien.orbien.core.transport.direct;

import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 独立隧道字节流分块编解码。
 * <p>
 * type=0 表示明文，1/2/3 为 Snappy/LZ4/Zstd 压缩块。
 */
public final class DirectTunnelChunkCodec {

    static final int HEADER_LENGTH = 5;
    static final byte TYPE_RAW = 0;
    static final int MAX_CHUNK_BODY = 10 * 1024 * 1024;

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DirectTunnelChunkCodec.class);

    private DirectTunnelChunkCodec() {
    }

    public static ByteBuf encode(Channel channel, ByteBuf payload, CompressionType algorithm) {
        int originalBytes = payload != null ? payload.readableBytes() : 0;
        CompressionType configured = algorithm != null ? algorithm : CompressionType.NONE;
        if (payload == null || originalBytes == 0) {
            return wrapRaw(channel, payload, originalBytes);
        }
        if (!configured.isCompressed() || originalBytes < TmspPayloadCompressor.MIN_COMPRESSIBLE_LENGTH) {
            if (logger.isDebugEnabled()) {
                String reason = !configured.isCompressed() ? "compress_disabled" : "below_threshold";
                logger.debug("[压缩] 独立隧道 跳过压缩 算法={} 原始字节={} 原因={}",
                        describeDirectAlgorithm(configured), originalBytes, describeDirectReason(reason));
            }
            return wrapRaw(channel, payload, originalBytes);
        }
        ByteBuf compressed = TmspPayloadCompressor.compressTransferBlock(channel, payload, configured);
        try {
            int compressedBytes = compressed.readableBytes();
            if (logger.isDebugEnabled()) {
                logger.debug("[压缩] 独立隧道 压缩完成 算法={} 原始字节={} 压缩后字节={} 压缩率={}",
                        describeDirectAlgorithm(configured), originalBytes, compressedBytes,
                        TmspPayloadCompressor.formatCompressionRatioPercent(originalBytes, compressedBytes));
            }
            return wrapBody(channel, (byte) configured.getValue(), compressed);
        } finally {
            compressed.release();
        }
    }

    public static ByteBuf decodeBody(Channel channel, byte type, ByteBuf body) {
        if (type == TYPE_RAW) {
            if (logger.isDebugEnabled()) {
                logger.debug("[解压] 独立隧道 跳过解压 线上字节={} 原因=明文分块", body.readableBytes());
            }
            return body.retain();
        }
        CompressionType algorithm = CompressionType.findByValue(type);
        if (!algorithm.isCompressed()) {
            return body.retain();
        }
        int compressedBytes = body.readableBytes();
        ByteBuf decompressed = TmspPayloadCompressor.decompressTransferBlock(channel, body, algorithm);
        if (logger.isDebugEnabled()) {
            logger.debug("[解压] 独立隧道 解压完成 算法={} 压缩字节={} 解压后字节={} 压缩率={}",
                    describeDirectAlgorithm(algorithm), compressedBytes, decompressed.readableBytes(),
                    TmspPayloadCompressor.formatCompressionRatioPercent(
                            decompressed.readableBytes(), compressedBytes));
        }
        return decompressed;
    }

    private static String describeDirectAlgorithm(CompressionType algorithm) {
        if (algorithm == null || !algorithm.isCompressed()) {
            return "无";
        }
        return algorithm.toConfigValue();
    }

    private static String describeDirectReason(String reason) {
        return switch (reason) {
            case "compress_disabled" -> "压缩已关闭";
            case "below_threshold" -> "低于压缩阈值";
            default -> reason;
        };
    }

    public static void validateBodyLength(int bodyLen) {
        if (bodyLen < 0 || bodyLen > MAX_CHUNK_BODY) {
            throw new CorruptedFrameException("invalid direct tunnel chunk length: " + bodyLen);
        }
    }

    private static ByteBuf wrapRaw(Channel channel, ByteBuf payload, int len) {
        ByteBuf out = channel.alloc().buffer(HEADER_LENGTH + len);
        out.writeByte(TYPE_RAW);
        out.writeInt(len);
        if (len > 0) {
            out.writeBytes(payload, payload.readerIndex(), len);
        }
        return out;
    }

    private static ByteBuf wrapBody(Channel channel, byte type, ByteBuf body) {
        int len = body.readableBytes();
        ByteBuf out = channel.alloc().buffer(HEADER_LENGTH + len);
        out.writeByte(type);
        out.writeInt(len);
        out.writeBytes(body);
        return out;
    }
}
