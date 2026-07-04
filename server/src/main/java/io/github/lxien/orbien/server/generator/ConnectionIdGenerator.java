package io.github.lxien.orbien.server.generator;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
@Component
public final class ConnectionIdGenerator {
    private static final AtomicInteger nextId = new AtomicInteger(2);

    public  int nextConnId() {
        int id = nextId.getAndAdd(2);
        if (id < 0) {
            throw new IllegalStateException("Connection ID overflow");
        }
        return id;
    }
}