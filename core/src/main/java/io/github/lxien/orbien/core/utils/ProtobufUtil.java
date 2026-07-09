package io.github.lxien.orbien.core.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;

public class ProtobufUtil {
    public static <T extends MessageLite> ByteBuf toByteBuf(T message, ByteBufAllocator alloc) {
        ByteBuf buf = alloc.buffer();
        try (ByteBufOutputStream out = new ByteBufOutputStream(buf,false)) {
            message.writeTo(out);
            return buf;
        } catch (IOException e) {
            ReferenceCountUtil.release(buf);
            throw new RuntimeException("Protobuf 编码失败", e);
        }
    }
    public static <T extends Message> T parseFrom(ByteBuf payload, Parser<T> parser) {
        if (payload == null || !payload.isReadable()) {
            throw new IllegalArgumentException("payload 为空");
        }
        int readerIndex = payload.readerIndex();
        try {
            return parser.parseFrom(new ByteBufInputStream(payload, false));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("解析 Protobuf 失败", e);
        } finally {
            payload.readerIndex(readerIndex);
        }
    }
}