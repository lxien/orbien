package io.github.lxien.orbien.core.transport;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchExecutorQueue<T> {
    private static final int DEFAULT_QUEUE_SIZE = 128;
    private final Queue<T> queue;
    private final AtomicBoolean scheduled;
    private final int chunkSize;
    /**
     * 如果5ms都没有包到达，直接全部刷新
     */
    private final long flushTimeoutMillis = 5;
    private volatile long lastMessageTime;

    public BatchExecutorQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    public BatchExecutorQueue(int chunkSize) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.scheduled = new AtomicBoolean(false);
        this.chunkSize = chunkSize;
        this.lastMessageTime = System.currentTimeMillis();
    }

    public void enqueue(T message, Executor executor) {
        queue.add(message);
        lastMessageTime = System.currentTimeMillis();
        scheduleFlush(executor);
    }

    protected void scheduleFlush(Executor executor) {
        if (scheduled.compareAndSet(false, true)) {
            executor.execute(() -> this.run(executor));
        }
    }

    private void run(Executor executor) {
        try {
            Queue<T> snapshot = new LinkedList<>();
            T item;
            while ((item = queue.poll()) != null) {
                snapshot.add(item);
            }
            int i = 0;
            boolean flushedOnce = false;
            while ((item = snapshot.poll()) != null) {
                if (snapshot.isEmpty()) {
                    flushedOnce = false;
                    break;
                }
                if (i == chunkSize || System.currentTimeMillis() - lastMessageTime > flushTimeoutMillis) {
                    i = 0;
                    flush(item);
                    flushedOnce = true;
                } else {
                    prepare(item);
                    i++;
                }
            }
            if (!flushedOnce && item != null) {
                flush(item);
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush(executor);
            }
        }
    }

    protected void prepare(T item) {
    }

    protected void flush(T item) {
    }
}