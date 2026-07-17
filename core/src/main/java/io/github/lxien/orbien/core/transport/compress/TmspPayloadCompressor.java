package io.github.lxien.orbien.core.transport.compress;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


public final class TmspPayloadCompressor {

    public static final int MIN_COMPRESSIBLE_LENGTH = 1024;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TmspPayloadCompressor.class);

    private TmspPayloadCompressor() {
    }

    /**
     * 构建 STREAM_DATA 帧，按代理配置决定是否压缩 payload。
     */
    public static TMSPFrame encodeStreamData(Channel channel, int streamId, ByteBuf payload, CompressionType algorithm) {
        int originalBytes = payload != null ? payload.readableBytes() : 0;
        CompressionType configured = algorithm != null ? algorithm : CompressionType.NONE;

        if (payload == null || originalBytes == 0) {
            logEncodeSkipped(streamId, configured, originalBytes, "empty_payload");
            return new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload != null ? payload.retain() : channel.alloc().buffer(0));
        }
        if (!configured.isCompressed()) {
            logEncodeSkipped(streamId, configured, originalBytes, "compress_disabled");
            return new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload.retain());
        }
        if (originalBytes < MIN_COMPRESSIBLE_LENGTH) {
            logEncodeSkipped(streamId, configured, originalBytes, "below_threshold");
            return new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload.retain());
        }

        ByteBuf compressed = compressBlock(channel, payload, configured);
        int compressedBytes = compressed.readableBytes();
        if (compressedBytes >= originalBytes) {
            compressed.release();
            logEncodeSkipped(streamId, configured, originalBytes, "expansion");
            return new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload.retain());
        }
        logEncoded(streamId, configured, originalBytes, compressedBytes);

        TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, compressed);
        frame.setCompressType(configured.getFlag());
        return frame;
    }

    /**
     * 解码 STREAM_DATA payload
     */
    public static ForwardPayload decodeForForward(Channel channel, TMSPFrame frame) {
        int streamId = frame != null ? frame.getStreamId() : -1;
        if (frame == null || frame.getPayload() == null) {
            return new ForwardPayload(channel.alloc().buffer(0), false);
        }
        int wireBytes = frame.getPayload().readableBytes();
        if (!frame.isCompressed()) {
            logDecodeSkipped(streamId, wireBytes, "frame_not_compressed");
            return new ForwardPayload(frame.getPayload(), true);
        }
        CompressionType algorithm = CompressionType.fromFlag(frame.getFlags());
        if (!algorithm.isCompressed()) {
            logDecodeSkipped(streamId, wireBytes, "algorithm_none");
            return new ForwardPayload(frame.getPayload(), true);
        }

        int compressedBytes = wireBytes;
        ByteBuf decompressed = decompressBlock(channel, frame.getPayload(), algorithm);
        int decompressedBytes = decompressed.readableBytes();
        logDecoded(streamId, algorithm, compressedBytes, decompressedBytes);
        return new ForwardPayload(decompressed, false);
    }

    public record ForwardPayload(ByteBuf buf, boolean sharedWithInbound) {
        public void releaseIfOwned() {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(buf);
            }
        }
    }

    public static void applyStreamFlags(TMSPFrame frame, CompressionType algorithm) {
        CompressionType effective = algorithm != null ? algorithm : CompressionType.NONE;
        if (effective.isCompressed()) {
            frame.setCompressType(effective.getFlag());
            if (logger.isDebugEnabled()) {
                logger.debug("[压缩] 流协商 流ID={} 算法={} 状态=已开启", frame.getStreamId(), describeAlgorithm(effective));
            }
        } else {
            frame.setCompressed(false);
            frame.setCompressType(TMSP.COMPRESS_ALGO_NONE);
            if (logger.isDebugEnabled()) {
                logger.debug("[压缩] 流协商 流ID={} 算法=无 状态=未开启", frame.getStreamId());
            }
        }
    }

    private static void logEncodeSkipped(int streamId, CompressionType algorithm, int originalBytes, String reason) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("[压缩] 流ID={} 跳过压缩 算法={} 原始字节={} 原因={}",
                streamId, describeAlgorithm(algorithm), originalBytes, describeReason(reason));
    }

    private static void logEncoded(int streamId, CompressionType algorithm, int originalBytes, int compressedBytes) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("[压缩] 流ID={} 压缩完成 算法={} 原始字节={} 压缩后字节={} 压缩率={}",
                streamId, describeAlgorithm(algorithm), originalBytes, compressedBytes,
                formatCompressionRatioPercent(originalBytes, compressedBytes));
    }

    private static void logDecodeSkipped(int streamId, int wireBytes, String reason) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("[解压] 流ID={} 跳过解压 线上字节={} 原因={}", streamId, wireBytes, describeReason(reason));
    }

    private static void logDecoded(int streamId, CompressionType algorithm, int compressedBytes, int decompressedBytes) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("[解压] 流ID={} 解压完成 算法={} 压缩字节={} 解压后字节={} 压缩率={}",
                streamId, describeAlgorithm(algorithm), compressedBytes, decompressedBytes,
                formatCompressionRatioPercent(decompressedBytes, compressedBytes));
    }

    /**
     * 压缩率：相对原始数据减少的体积百分比，正值表示压缩有效。
     */
    public static String formatCompressionRatioPercent(int originalBytes, int compressedBytes) {
        if (originalBytes <= 0) {
            return "0.00%";
        }
        double ratio = (1.0 - (double) compressedBytes / originalBytes) * 100.0;
        return String.format("%.2f%%", ratio);
    }

    private static String describeAlgorithm(CompressionType algorithm) {
        if (algorithm == null || !algorithm.isCompressed()) {
            return "无";
        }
        return algorithm.toConfigValue();
    }

    private static String describeReason(String reason) {
        return switch (reason) {
            case "empty_payload" -> "载荷为空";
            case "compress_disabled" -> "压缩已关闭";
            case "below_threshold" -> "低于压缩阈值";
            case "frame_not_compressed" -> "帧未标记压缩";
            case "algorithm_none" -> "算法为无";
            case "raw_chunk" -> "明文分块";
            case "expansion" -> "压缩后体积未减小";
            default -> reason;
        };
    }

    /**
     * 压缩单个数据块（Snappy/LZ4 块格式），供独立隧道等字节流场景复用。
     */
    public static ByteBuf compressTransferBlock(Channel channel, ByteBuf in, CompressionType algorithm) {
        return compressBlock(channel, in, algorithm);
    }

    /**
     * 解压单个数据块，算法由协商结果指定。
     */
    public static ByteBuf decompressTransferBlock(Channel channel, ByteBuf in, CompressionType algorithm) {
        return decompressBlock(channel, in, algorithm);
    }

    /**
     * 解码 TMSP 控制消息 payload；未压缩时返回共享引用。
     */
    public static ControlPayload decodeControlPayload(Channel channel, TMSPFrame frame) {
        if (frame == null || frame.getPayload() == null) {
            return new ControlPayload(channel.alloc().buffer(0), true);
        }
        if (!frame.isCompressed()) {
            return new ControlPayload(frame.getPayload(), false);
        }
        CompressionType algorithm = CompressionType.fromFlag(frame.getFlags());
        if (!algorithm.isCompressed()) {
            return new ControlPayload(frame.getPayload(), false);
        }
        int compressedBytes = frame.getPayload().readableBytes();
        ByteBuf decompressed = decompressBlock(channel, frame.getPayload(), algorithm);
        if (logger.isDebugEnabled()) {
            logger.debug("[解压] 控制消息 类型={} 算法={} 压缩字节={} 解压后字节={} 压缩率={}",
                    frame.getMsgType(), describeAlgorithm(algorithm), compressedBytes, decompressed.readableBytes(),
                    formatCompressionRatioPercent(decompressed.readableBytes(), compressedBytes));
        }
        return new ControlPayload(decompressed, true);
    }

    public record ControlPayload(ByteBuf buf, boolean owned) {
        public void releaseIfOwned() {
            if (owned) {
                ReferenceCountUtil.release(buf);
            }
        }
    }

    /**
     * 压缩 TMSP 控制消息 payload，并设置帧级压缩标志。
     */
    public static void encodeControlPayload(Channel channel, TMSPFrame frame, CompressionType algorithm) {
        if (frame == null || frame.getPayload() == null) {
            return;
        }
        CompressionType configured = algorithm != null ? algorithm : CompressionType.NONE;
        ByteBuf payload = frame.getPayload();
        int originalBytes = payload.readableBytes();
        if (!configured.isCompressed() || originalBytes < MIN_COMPRESSIBLE_LENGTH) {
            return;
        }
        ByteBuf compressed = compressBlock(channel, payload, configured);
        if (compressed.readableBytes() >= originalBytes) {
            compressed.release();
            if (logger.isDebugEnabled()) {
                logger.debug("[压缩] 控制消息 类型={} 跳过压缩 算法={} 原始字节={} 原因=压缩后体积未减小",
                        frame.getMsgType(), describeAlgorithm(configured), originalBytes);
            }
            return;
        }
        ReferenceCountUtil.release(payload);
        frame.setPayload(compressed);
        frame.setCompressType(configured.getFlag());
        if (logger.isDebugEnabled()) {
            logger.debug("[压缩] 控制消息 类型={} 算法={} 原始字节={} 压缩后字节={} 压缩率={}",
                    frame.getMsgType(), describeAlgorithm(configured), originalBytes, compressed.readableBytes(),
                    formatCompressionRatioPercent(originalBytes, compressed.readableBytes()));
        }
    }

    private static ByteBuf compressBlock(Channel channel, ByteBuf in, CompressionType algorithm) {
        return switch (algorithm) {
            case SNAPPY -> SnappyCompressor.compressBlock(channel, in);
            case LZ4 -> Lz4Compressor.compressBlock(channel, in);
            case ZSTD -> ZstdCompressor.compressBlock(channel, in);
            default -> in.retain();
        };
    }

    private static ByteBuf decompressBlock(Channel channel, ByteBuf in, CompressionType algorithm) {
        return switch (algorithm) {
            case SNAPPY -> SnappyCompressor.decompressBlock(channel, in);
            case LZ4 -> Lz4Compressor.decompressBlock(channel, in);
            case ZSTD -> ZstdCompressor.decompressBlock(channel, in);
            default -> in.retain();
        };
    }
}
