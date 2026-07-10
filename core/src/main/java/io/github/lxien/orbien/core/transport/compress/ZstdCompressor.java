package io.github.lxien.orbien.core.transport.compress;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.compression.CompressionException;

import java.util.function.Consumer;

/**
 * Zstd 块压缩实现，用于 TMSP 帧级 payload 压缩。
 * <p>
 * 格式：{@code [int 原始长度][Zstd 压缩字节]}
 */
public class ZstdCompressor implements Compressor {

    @Override
    public void compress(Channel channel, ByteBuf in, Consumer<ByteBuf> consumer, int level) {
        ByteBuf out = compressBlock(channel, in, level);
        consumer.accept(out);
    }

    @Override
    public ByteBuf decompress(Channel channel, ByteBuf in) {
        return decompressBlock(channel, in);
    }

    public static ByteBuf compressBlock(Channel channel, ByteBuf in) {
        return compressBlock(channel, in, Zstd.defaultCompressionLevel());
    }

    public static ByteBuf compressBlock(Channel channel, ByteBuf in, int level) {
        int originalLen = in.readableBytes();
        if (originalLen == 0) {
            return channel.alloc().buffer(0);
        }
        byte[] input = new byte[originalLen];
        in.getBytes(in.readerIndex(), input);

        int effectiveLevel = level > 0 ? level : Zstd.defaultCompressionLevel();
        byte[] compressed = Zstd.compress(input, effectiveLevel);

        ByteBuf out = channel.alloc().buffer(4 + compressed.length);
        out.writeInt(originalLen);
        out.writeBytes(compressed);
        return out;
    }

    public static ByteBuf decompressBlock(Channel channel, ByteBuf in) {
        if (in.readableBytes() < 4) {
            throw new CompressionException("Zstd payload too short");
        }
        int originalLen = in.readInt();
        if (originalLen < 0) {
            throw new CompressionException("Invalid Zstd original length: " + originalLen);
        }
        int compressedLen = in.readableBytes();
        if (compressedLen == 0) {
            return channel.alloc().buffer(0);
        }
        byte[] compressed = new byte[compressedLen];
        in.readBytes(compressed);

        long restoredLen = Zstd.decompressedSize(compressed);
        if (restoredLen < 0) {
            throw new CompressionException("Invalid Zstd frame content size");
        }
        if (restoredLen != originalLen) {
            throw new CompressionException(
                    "Zstd original length mismatch: header=" + originalLen + " frame=" + restoredLen);
        }
        byte[] restored = Zstd.decompress(compressed, originalLen);
        if (restored.length != originalLen) {
            throw new CompressionException(
                    "Zstd decompressed length mismatch: expected=" + originalLen + " actual=" + restored.length);
        }
        return channel.alloc().buffer(originalLen).writeBytes(restored);
    }
}
