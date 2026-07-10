package io.github.lxien.orbien.core.transport.compress;

import io.github.lxien.orbien.core.message.TMSP;

/**
 * TMSP 帧级压缩算法。
 */
public enum CompressionType {
    NONE(0, TMSP.COMPRESS_ALGO_NONE),
    SNAPPY(1, TMSP.COMPRESS_ALGO_SNAPPY),
    LZ4(2, TMSP.COMPRESS_ALGO_LZ4),
    ZSTD(3, TMSP.COMPRESS_ALGO_ZSTD);

    public static final CompressionType DEFAULT = SNAPPY;

    private final int value;
    private final byte flag;

    CompressionType(int value, byte flag) {
        this.value = value;
        this.flag = flag;
    }

    public int getValue() {
        return value;
    }

    public byte getFlag() {
        return flag;
    }

    public static CompressionType of(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        return switch (name.trim().toLowerCase()) {
            case "snappy" -> SNAPPY;
            case "lz4" -> LZ4;
            case "zstd" -> ZSTD;
            default -> NONE;
        };
    }

    public static CompressionType findByValue(int value) {
        for (CompressionType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NONE;
    }

    public static CompressionType fromFlag(byte flags) {
        byte compressFlag = (byte) (flags & TMSP.COMPRESS_ALGO_MASK);
        for (CompressionType type : values()) {
            if (type.flag == compressFlag) {
                return type;
            }
        }
        return NONE;
    }

    public boolean isCompressed() {
        return this != NONE;
    }

    public String toConfigValue() {
        return name().toLowerCase();
    }
}
