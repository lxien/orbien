package io.github.lxien.orbien.core.transport.compress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.compression.CompressionException;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.util.function.Consumer;

/**
 * LZ4 块压缩实现，用于 TMSP 帧级 payload 压缩。
 * <p>
 * 格式：{@code [int 原始长度][LZ4 压缩字节]}
 * <p>
 * 不使用 {@link LZ4Factory#fastestInstance()}：其在运行时按类名动态加载多种实现，
 * GraalVM native image 下容易因反射类未注册而初始化失败。
 */
public class Lz4Compressor implements Compressor {

    private static final class Engines {
        static final LZ4Factory LZ4;

        static {
            LZ4Factory factory;
            try {
                factory = LZ4Factory.nativeInstance();
            } catch (Throwable ignored) {
                factory = LZ4Factory.safeInstance();
            }
            LZ4 = factory;
        }
    }

    @Override
    public void compress(Channel channel, ByteBuf in, Consumer<ByteBuf> consumer, int level) {
        ByteBuf out = compressBlock(channel, in);
        consumer.accept(out);
    }

    @Override
    public ByteBuf decompress(Channel channel, ByteBuf in) {
        return decompressBlock(channel, in);
    }

    public static ByteBuf compressBlock(Channel channel, ByteBuf in) {
        int originalLen = in.readableBytes();
        if (originalLen == 0) {
            return channel.alloc().buffer(0);
        }
        byte[] input = new byte[originalLen];
        in.getBytes(in.readerIndex(), input);

        LZ4Compressor compressor = Engines.LZ4.fastCompressor();
        int maxCompressed = compressor.maxCompressedLength(originalLen);
        byte[] compressed = new byte[maxCompressed];
        int compressedLen = compressor.compress(input, 0, originalLen, compressed, 0, maxCompressed);

        ByteBuf out = channel.alloc().buffer(4 + compressedLen);
        out.writeInt(originalLen);
        out.writeBytes(compressed, 0, compressedLen);
        return out;
    }

    public static ByteBuf decompressBlock(Channel channel, ByteBuf in) {
        if (in.readableBytes() < 4) {
            throw new CompressionException("LZ4 payload too short");
        }
        int originalLen = in.readInt();
        if (originalLen < 0) {
            throw new CompressionException("Invalid LZ4 original length: " + originalLen);
        }
        int compressedLen = in.readableBytes();
        if (compressedLen == 0) {
            return channel.alloc().buffer(0);
        }
        byte[] compressed = new byte[compressedLen];
        in.readBytes(compressed);

        LZ4FastDecompressor decompressor = Engines.LZ4.fastDecompressor();
        byte[] restored = new byte[originalLen];
        // 返回值是从 compressed 中读取的字节数，不是解压后的长度
        int compressedBytesRead = decompressor.decompress(compressed, 0, restored, 0, originalLen);
        if (compressedBytesRead != compressedLen) {
            throw new CompressionException(
                    "LZ4 compressed block size mismatch: wire=" + compressedLen + " consumed=" + compressedBytesRead);
        }
        return channel.alloc().buffer(originalLen).writeBytes(restored);
    }
}
