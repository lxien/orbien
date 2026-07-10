package io.github.lxien.orbien.core.transport.compress;

import java.util.EnumMap;

public class CompressorFactory {
    private static final EnumMap<CompressionType, Compressor> COMPRESSORS;

    static {
        COMPRESSORS = new EnumMap<>(CompressionType.class);
        COMPRESSORS.put(CompressionType.SNAPPY, new SnappyCompressor());
        COMPRESSORS.put(CompressionType.LZ4, new Lz4Compressor());
        COMPRESSORS.put(CompressionType.ZSTD, new ZstdCompressor());
    }

    public static Compressor getCompressor(CompressionType type) {
        return COMPRESSORS.get(type);
    }

}