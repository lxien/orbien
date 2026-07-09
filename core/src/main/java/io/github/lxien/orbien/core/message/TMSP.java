package io.github.lxien.orbien.core.message;

/**
 * TMSP (Tunnel Multiplexed Stream Protocol)
 * TCP多路复用流协议
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
    public static final byte FLAG_COMPRESSED = 0x01;  // 0000 0001 加密
    public static final byte FLAG_ENCRYPTED = 0x02;   // 0000 0010 压缩
    public static final byte FLAG_MUX = 0x04;        //  0000 0100 多路复用
    public static final byte FLAG_DATAGRAM = 0x08;   //  0000 1000 数据报(UDP)

    // bit 3~5 用来表示压缩算法类型（最多支持 8 种）
    public static final byte COMPRESS_NONE = 0x00;   // 0000 0000
    public static final byte COMPRESS_LZ4 = 0x08;   // 0000 1000  (bit 3)
    public static final byte COMPRESS_SNAPPY = 0x10;   // 0001 0000  (bit 4)
    public static final byte COMPRESS_MASK = 0x38;   // 0011 1000  (bit 3~5，用于掩码)

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
