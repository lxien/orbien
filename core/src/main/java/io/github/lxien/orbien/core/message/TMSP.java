package io.github.lxien.orbien.core.message;

/**
 * TMSP (Tunnel Multiplexed Stream Protocol)
 */
public class TMSP {
    public static final String PROTOCOL_NAME = "TMSP";
    public static final int MAGIC = 0x544D5350; // 'T','M','S','P'
    public static final byte VERSION = 0x10;     // 1.0

    public static final byte MSG_AUTH = 0x01;
    public static final byte MSG_AUTH_RESP = 0x02;
    public static final byte MSG_PING = 0x03;
    public static final byte MSG_PONG = 0x04;
    public static final byte MSG_GOAWAY = 0x05;
    public static final byte MSG_ERROR = 0x06;

    public static final byte MSG_CONNECTION_CREATE = 0x07;
    public static final byte MSG_CONNECTION_CREATE_RESP = 0x08;

    public static final byte MSG_SERVICE_HEALTH_REPORT = 0x09;
    public static final byte MSG_PROXY_REPORT_REQ = 0x10;
    public static final byte MSG_PROXY_REPORT_RESP = 0x11;
    public static final byte MSG_CONFIG_SYNC = 0x12;

    public static final byte MSG_STREAM_OPEN = 0x20;
    public static final byte MSG_STREAM_OPEN_RESP = 0x21;
    public static final byte MSG_STREAM_CLOSE = 0x22;
    public static final byte MSG_STREAM_RESET = 0x23;
    public static final byte MSG_STREAM_DATA = 0x24;

    public static final byte MSG_STREAM_PAUSE = 0x25;
    public static final byte MSG_STREAM_RESUME = 0x26;

    public static final byte MSG_FILE_LIST_REQ = 0x30;
    public static final byte MSG_FILE_LIST_RESP = 0x31;
    public static final byte MSG_FILE_TRANSFER_INIT = 0x34;
    public static final byte MSG_FILE_CHUNK = 0x35;
    public static final byte MSG_FILE_TRANSFER_DONE = 0x37;
    public static final byte MSG_FILE_OP_REQ = 0x38;
    public static final byte MSG_FILE_OP_RESP = 0x39;

    // flags 位掩码
    public static final byte FLAG_COMPRESSED = 0x01;  // bit 0：payload 是否压缩
    public static final byte FLAG_ENCRYPTED = 0x02;   // bit 1：流/隧道加密标记
    public static final byte FLAG_MUX = 0x04;        // bit 2：多路复用
    public static final byte FLAG_DATAGRAM = 0x08;   // bit 3：UDP 数据报

    // bit 4~6 表示压缩算法（与 FLAG_DATAGRAM 分离，避免位冲突）
    public static final byte COMPRESS_ALGO_NONE = 0x00;
    public static final byte COMPRESS_ALGO_SNAPPY = 0x10;   // bit 4
    public static final byte COMPRESS_ALGO_LZ4 = 0x20;      // bit 5
    public static final byte COMPRESS_ALGO_ZSTD = 0x40;     // bit 6
    public static final byte COMPRESS_ALGO_MASK = 0x70;     // bit 4~6

    /**
     * 获取主版本号
     *
     * @param version 版本字节
     * @return 主版本号
     */
    public static int getMajorVersion(byte version) {
        return (version >> 4) & 0x0F;
    }

    /**
     * 获取次版本号
     *
     * @param version 版本字节
     * @return 次版本号
     */
    public static int getMinorVersion(byte version) {
        return version & 0x0F;
    }

    /**
     * 格式化版本号
     *
     * @param version 版本字节
     * @return 如 "1.0"
     */
    public static String formatVersion(byte version) {
        return getMajorVersion(version) + "." + getMinorVersion(version);
    }

    /**
     * 创建版本号
     *
     * @param major 主版本 (0-15)
     * @param minor 次版本 (0-15)
     * @return 版本字节
     */
    public static byte createVersion(int major, int minor) {
        return (byte) (((major & 0x0F) << 4) | (minor & 0x0F));
    }
}
