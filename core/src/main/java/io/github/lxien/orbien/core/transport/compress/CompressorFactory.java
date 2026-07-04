package io.github.lxien.orbien.core.transport.compress;

import java.util.EnumMap;

public class CompressorFactory {
    private static final EnumMap<CompressionType, Compressor> COMPRESSORS;

    static {
        COMPRESSORS = new EnumMap<>(CompressionType.class);
        COMPRESSORS.put(CompressionType.SNAPPY, new SnappyCompressor());
    }

    public static Compressor getCompressor(CompressionType type) {
        return COMPRESSORS.get(type);
    }

}