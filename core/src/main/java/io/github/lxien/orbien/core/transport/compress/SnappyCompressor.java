package io.github.lxien.orbien.core.transport.compress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.compression.CompressionException;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Snappy 块压缩实现，用于 TMSP 帧级 payload 压缩。
 * <p>
 * 使用 {@code snappy-java}（JNI）替代 Netty 纯 Java {@link io.netty.handler.codec.compression.Snappy}，
 * 避免静态/共享实例的线程安全问题，并显著提升吞吐。
 * <p>
 * 格式：{@code [int 原始长度][Snappy 压缩字节]}
 */
public class SnappyCompressor implements Compressor {

    protected static final int MIN_COMPRESSIBLE_LENGTH = 1024;

    @Override
    public void compress(Channel channel, ByteBuf in, Consumer<ByteBuf> consumer, int level) {
        int dataLength = in.readableBytes();
        if (dataLength == 0) {
            return;
        }
        if (dataLength < MIN_COMPRESSIBLE_LENGTH) {
            consumer.accept(in.retain());
            return;
        }
        consumer.accept(compressBlock(channel, in));
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
        try {
            byte[] compressed = Snappy.compress(input);
            ByteBuf out = channel.alloc().buffer(4 + compressed.length);
            out.writeInt(originalLen);
            out.writeBytes(compressed);
            return out;
        } catch (IOException e) {
            throw new CompressionException("Snappy compress failed", e);
        }
    }

    public static ByteBuf decompressBlock(Channel channel, ByteBuf in) {
        if (in.readableBytes() < 4) {
            throw new CompressionException("Snappy payload too short");
        }
        int originalLen = in.readInt();
        if (originalLen < 0) {
            throw new CompressionException("Invalid Snappy original length: " + originalLen);
        }
        int compressedLen = in.readableBytes();
        if (compressedLen == 0) {
            return channel.alloc().buffer(0);
        }
        byte[] compressed = new byte[compressedLen];
        in.readBytes(compressed);
        try {
            byte[] restored = new byte[originalLen];
            int written = Snappy.uncompress(compressed, 0, compressedLen, restored, 0);
            if (written != originalLen) {
                throw new CompressionException(
                        "Snappy decompressed length mismatch: expected=" + originalLen + " actual=" + written);
            }
            return channel.alloc().buffer(originalLen).writeBytes(restored);
        } catch (IOException e) {
            throw new CompressionException("Snappy decompress failed", e);
        }
    }
}
