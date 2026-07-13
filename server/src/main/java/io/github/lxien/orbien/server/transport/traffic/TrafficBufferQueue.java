package io.github.lxien.orbien.server.transport.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayDeque;

/**
 * 限流等待队列，支持从队首按字节切片消费。
 */
final class TrafficBufferQueue {

    private final ArrayDeque<ByteBuf> buffers = new ArrayDeque<>();
    private int frameCount;
    private long byteCount;

    boolean isEmpty() {
        return buffers.isEmpty();
    }

    int size() {
        return frameCount;
    }

    long bytes() {
        return byteCount;
    }

    /**
     * @return false 表示超出上限，payload 仍由调用方负责释放
     */
    boolean offer(ByteBuf payload, int maxFrames, long maxBytes) {
        int bytes = payload.readableBytes();
        if (frameCount + 1 > maxFrames || byteCount + bytes > maxBytes) {
            return false;
        }
        buffers.offer(payload);
        frameCount++;
        byteCount += bytes;
        return true;
    }

    ByteBuf peek() {
        return buffers.peek();
    }

    /**
     * 从队首取出最多 {@code maxBytes} 字节；返回的切片由调用方负责释放或写出
     * <p>返回值与队列内剩余 buffer 互不共享同一派生对象，clear 不会误伤 in-flight 写出
     */
    ByteBuf pollSlice(int maxBytes) {
        ByteBuf front = buffers.peek();
        if (front == null) {
            return null;
        }
        if (!front.isReadable()) {
            buffers.poll();
            frameCount--;
            ReferenceCountUtil.release(front);
            return null;
        }
        int readable = front.readableBytes();
        if (readable <= maxBytes) {
            buffers.poll();
            frameCount--;
            byteCount -= readable;
            return front;
        }
        // 拆成两个独立 retainedSlice，再释放原来的 front，切断 parent 共享
        int readerIndex = front.readerIndex();
        ByteBuf out = front.retainedSlice(readerIndex, maxBytes);
        ByteBuf rest = front.retainedSlice(readerIndex + maxBytes, readable - maxBytes);
        buffers.poll();
        ReferenceCountUtil.release(front);
        buffers.offerFirst(rest);
        byteCount -= maxBytes;
        return out;
    }

    void clear() {
        ByteBuf buf;
        while ((buf = buffers.poll()) != null) {
            ReferenceCountUtil.safeRelease(buf);
        }
        frameCount = 0;
        byteCount = 0;
    }
}
