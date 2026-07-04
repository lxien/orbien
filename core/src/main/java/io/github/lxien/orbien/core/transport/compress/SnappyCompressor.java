package io.github.lxien.orbien.core.transport.compress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.compression.CompressionException;
import io.netty.handler.codec.compression.Snappy;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.function.Consumer;

/**
 * Snappy 压缩器实现
 */
public class SnappyCompressor implements Compressor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SnappyCompressor.class);

    /**
     * 最小可压缩数据长度
     */
    protected static final int MIN_COMPRESSIBLE_LENGTH = 1024;

    /**
     * 最大未压缩块大小限制
     */
    private static final int MAX_UNCOMPRESSED_DATA_SIZE = 65536 + 4;

    /**
     * 最大压缩块大小限制
     */
    private static final int MAX_COMPRESSED_CHUNK_SIZE = 16777216 - 1;

    /**
     * Snappy 压缩算法实例
     */
    private final Snappy snappy = new Snappy();

    /**
     * 压缩数据
     *
     * @param channel  网络通道
     * @param in       输入数据缓冲区
     * @param consumer 压缩结果消费者
     * @param level    压缩级别（当前实现未使用）
     */
    @Override
    public void compress(Channel channel, ByteBuf in, Consumer<ByteBuf> consumer, int level) {
        int dataLength = in.readableBytes();
        if (dataLength == 0) {
            return;
        }

        if (dataLength > MIN_COMPRESSIBLE_LENGTH) {
            for (; ; ) {
                if (dataLength < MIN_COMPRESSIBLE_LENGTH) {
                    ByteBuf slice = in.readSlice(dataLength);
                    writeUnencodedChunk(channel, slice, dataLength, consumer);
                    break;
                }

                ByteBuf out = channel.alloc().buffer();
                final int lengthIdx = out.writerIndex() + 1;

                out.writeByte(0);
                out.writeMediumLE(0);

                if (dataLength > Short.MAX_VALUE) {
                    ByteBuf slice = in.readSlice(Short.MAX_VALUE);
                    snappy.encode(slice, out, Short.MAX_VALUE);
                    setChunkLength(out, lengthIdx);
                    consumer.accept(out);
                    dataLength -= Short.MAX_VALUE;
                } else {
                    ByteBuf slice = in.readSlice(dataLength);
                    snappy.encode(slice, out, dataLength);
                    setChunkLength(out, lengthIdx);
                    consumer.accept(out);
                    break;
                }
            }
        } else {
            writeUnencodedChunk(channel, in, dataLength, consumer);
        }
    }

    /**
     * 写入未压缩数据块
     *
     * @param channel    网络通道
     * @param in         输入数据缓冲区
     * @param dataLength 数据长度
     * @param consumer   结果消费者
     */
    private static void writeUnencodedChunk(Channel channel, ByteBuf in, int dataLength, Consumer<ByteBuf> consumer) {
        ByteBuf out = channel.alloc().buffer(1 + 3 + dataLength);
        try {
            out.writeByte(1);
            out.writeMediumLE(dataLength);
            out.writeBytes(in, dataLength);
            consumer.accept(out);
        } catch (Exception e) {
            out.release();
            throw new CompressionException("Write uncompressed chunk failed", e);
        }
    }

    /**
     * 解压数据
     *
     * @param channel 网络通道
     * @param in      输入数据缓冲区
     * @return 解压后的缓冲区
     */
    @Override
    public ByteBuf decompress(Channel channel, ByteBuf in) {
        if (in == null || !in.isReadable()) {
            return channel.alloc().buffer(0);
        }

        ByteBuf out = channel.alloc().buffer(Math.max(8192, in.readableBytes() * 2));

        try {
            while (in.isReadable()) {
                if (in.readableBytes() < 4) {
                    throw new CompressionException("Incomplete chunk header");
                }

                byte type = in.readByte();
                int chunkLength = in.readMediumLE();

                if (chunkLength < 0) {
                    throw new CompressionException("Invalid chunk length: " + chunkLength);
                }

                if (type == 0) {
                    if (chunkLength > MAX_COMPRESSED_CHUNK_SIZE) {
                        throw new CompressionException("Compressed chunk too large: " + chunkLength);
                    }
                    if (chunkLength > in.readableBytes()) {
                        throw new CompressionException("Not enough data for compressed chunk");
                    }

                    ByteBuf compressed = in.readSlice(chunkLength);
                    snappy.reset();
                    snappy.decode(compressed, out);

                } else if (type == 1) {
                    if (chunkLength > MAX_UNCOMPRESSED_DATA_SIZE) {
                        throw new CompressionException("Uncompressed chunk too large: " + chunkLength);
                    }
                    if (chunkLength > in.readableBytes()) {
                        throw new CompressionException("Not enough data for uncompressed chunk");
                    }

                    out.writeBytes(in, chunkLength);

                } else {
                    throw new CompressionException("Unknown or reserved chunk type: " + type);
                }
            }

            return out;
        } catch (Exception e) {
            out.release();
            throw new CompressionException("Snappy decompress failed", e);
        }
    }

    /**
     * 设置压缩块长度
     *
     * @param out       输出缓冲区
     * @param lengthIdx 长度字段索引
     */
    private static void setChunkLength(ByteBuf out, int lengthIdx) {
        int chunkLength = out.writerIndex() - lengthIdx - 3;
        if (chunkLength >>> 24 != 0) {
            throw new CompressionException("compressed data too large: " + chunkLength);
        }
        out.setMediumLE(lengthIdx, chunkLength);
    }
}