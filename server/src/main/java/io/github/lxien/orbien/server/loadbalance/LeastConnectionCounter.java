package io.github.lxien.orbien.server.loadbalance;

import io.github.lxien.orbien.core.utils.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class LeastConnectionCounter {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    public int get(String key) {
        LongAdder adder = counts.get(key);
        return adder == null ? 0 : adder.intValue();
    }

    public void inc(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        counts.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public void dec(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        LongAdder adder = counts.get(key);
        if (adder != null) {
            adder.decrement();
            if (adder.intValue() <= 0) {
                counts.remove(key, adder);
            }
        }
    }
}