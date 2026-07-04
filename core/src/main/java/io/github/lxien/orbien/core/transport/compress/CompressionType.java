/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.core.transport.compress;

import io.github.lxien.orbien.core.message.TMSP;

public enum CompressionType {
    /**
     * 不压缩
     */
    NONE(0, TMSP.COMPRESS_NONE),

    /**
     * LZ4 - 速度最快，默认
     */
    LZ4(1, TMSP.COMPRESS_LZ4),

    /**
     * Snappy - 兼容性好
     */
    SNAPPY(3, TMSP.COMPRESS_SNAPPY);

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

    /**
     * 根据名称获取
     */
    public static CompressionType of(String name) {
        if (name == null) return NONE;
        switch (name.trim().toUpperCase()) {
            case "LZ4":
                return LZ4;
            case "SNAPPY":
                return SNAPPY;
            default:
                return NONE;
        }
    }

    /**
     * 根据数值获取
     */
    public static CompressionType findByValue(int value) {
        for (CompressionType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NONE;
    }

    /**
     * 根据 flags 中的压缩标记获取类型
     */
    public static CompressionType fromFlag(byte flags) {
        byte compressFlag = (byte) (flags & TMSP.COMPRESS_MASK);

        for (CompressionType type : values()) {
            if (type.flag == compressFlag) {
                return type;
            }
        }
        return NONE;
    }

    /**
     * 是否需要压缩（NONE 以外的都算需要压缩）
     */
    public boolean isCompressed() {
        return this != NONE;
    }
}