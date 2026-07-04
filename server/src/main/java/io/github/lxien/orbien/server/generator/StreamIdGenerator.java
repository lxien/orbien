package io.github.lxien.orbien.server.generator;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
@Component
public final class StreamIdGenerator {
    private static final AtomicInteger nextId = new AtomicInteger(2);

    public  int nextStreamId() {
        // 每次+2，保持偶数
        int id = nextId.getAndAdd(2);
        if (id < 0) {
            throw new IllegalStateException("Stream ID overflow");
        }
        return id;
    }
}