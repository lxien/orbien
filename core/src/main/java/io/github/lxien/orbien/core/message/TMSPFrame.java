package io.github.lxien.orbien.core.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TMSPFrame implements ReferenceCounted {
    /**
     * 协议魔数
     */
    private int magic = TMSP.MAGIC;

    /**
     * 协议版本
     */
    private byte version = TMSP.VERSION;
    /**
     * 标记位
     */
    private byte flags;
    /**
     * 流ID - 标识所属的逻辑流
     */
    private int streamId;
    /**
     * 消息类型
     */
    private byte msgType;

    /**
     * 数据包
     */
    private ByteBuf payload;

    public TMSPFrame(byte msgType, ByteBuf payload) {
        this.msgType = msgType;
        this.payload = payload;
    }

    public TMSPFrame(int streamId, byte msgType) {
        this.streamId = streamId;
        this.msgType = msgType;
    }

    public TMSPFrame(int streamId, byte msgType, ByteBuf payload) {
        this.streamId = streamId;
        this.msgType = msgType;
        this.payload = payload;
    }

    /**
     * 判断是否压缩
     *
     * @return true=已压缩
     */
    public boolean isCompressed() {
        return (flags & TMSP.FLAG_COMPRESSED) != 0;
    }

    /**
     * 设置是否压缩
     *
     * @param compressed true=压缩，false=不压缩
     */
    public void setCompressed(boolean compressed) {
        if (compressed) {
            flags |= TMSP.FLAG_COMPRESSED;
        } else {
            flags &= ~TMSP.FLAG_COMPRESSED;
        }
    }

    /**
     * 判断是否加密
     *
     * @return true=已加密
     */
    public boolean isEncrypted() {
        return (flags & TMSP.FLAG_ENCRYPTED) != 0;
    }

    /**
     * 设置是否加密
     *
     * @param encrypted true=加密，false=不加密
     */
    public void setEncrypted(boolean encrypted) {
        if (encrypted) {
            flags |= TMSP.FLAG_ENCRYPTED;
        } else {
            flags &= ~TMSP.FLAG_ENCRYPTED;
        }
    }

    /**
     * 同时设置压缩和加密
     *
     * @param compress 是否压缩
     * @param encrypt  是否加密
     */
    public void setFlags(boolean compress, boolean encrypt) {
        byte newFlags = 0;
        if (compress) newFlags |= TMSP.FLAG_COMPRESSED;
        if (encrypt) newFlags |= TMSP.FLAG_ENCRYPTED;
        this.flags = newFlags;
    }

    /**
     * 判断是否既压缩又加密
     */
    public boolean isCompressedAndEncrypted() {
        return (flags & (TMSP.FLAG_COMPRESSED | TMSP.FLAG_ENCRYPTED))
                == (TMSP.FLAG_COMPRESSED | TMSP.FLAG_ENCRYPTED);
    }

    public boolean isMuxTunnel() {
        return (flags & TMSP.FLAG_MUX) != 0;
    }

    /**
     * 设置为共享隧道
     *
     * @param isMux 是否复用隧道
     */
    public void setMultiplexTunnel(boolean isMux) {
        if (isMux) {
            flags |= TMSP.FLAG_MUX;
        } else {
            flags &= ~TMSP.FLAG_MUX;
        }
    }

    public boolean isDatagram() {
        return (flags & TMSP.FLAG_DATAGRAM) != 0;
    }

    public void setDatagram(boolean datagram) {
        if (datagram) {
            flags |= TMSP.FLAG_DATAGRAM;
        } else {
            flags &= ~TMSP.FLAG_DATAGRAM;
        }
    }

    /**
     * 获取压缩算法类型
     */
    public byte getCompressType() {
        return (byte) (flags & TMSP.COMPRESS_MASK);
    }

    /**
     * 设置压缩算法类型
     */
    public void setCompressType(byte compressType) {
        // 先清除原来的压缩类型位
        flags &= ~TMSP.COMPRESS_MASK;
        // 设置新的压缩类型
        flags |= compressType;

        // 如果设置了非 NONE，则自动打开压缩标志
        if (compressType != TMSP.COMPRESS_NONE) {
            flags |= TMSP.FLAG_COMPRESSED;
        } else {
            flags &= ~TMSP.FLAG_COMPRESSED;
        }
    }

    public boolean isLz4() {
        return getCompressType() == TMSP.COMPRESS_LZ4;
    }

    public boolean isSnappy() {
        return getCompressType() == TMSP.COMPRESS_SNAPPY;
    }

    @Override
    public int refCnt() {
        return payload != null ? payload.refCnt() : 0;
    }

    @Override
    public ReferenceCounted retain() {
        if (payload != null) {
            payload.retain();
        }
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        if (payload != null) {
            payload.retain(increment);
        }
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        if (payload != null) {
            payload.touch();
        }
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if (payload != null) {
            payload.touch(hint);
        }
        return this;
    }

    @Override
    public boolean release() {
        if (payload != null) {
            return payload.release();
        }
        return false;
    }

    @Override
    public boolean release(int decrement) {
        if (payload != null) {
            return payload.release(decrement);
        }
        return false;
    }
}
