package io.github.lxien.orbien.server.web.monitor;

import io.netty.util.internal.PlatformDependent;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public final class DirectMemoryUtils {

    private static final String DIRECT = "direct";

    private DirectMemoryUtils() {
    }

    public static long usedDirectMemory() {
        try {
            return Math.max(0L, PlatformDependent.usedDirectMemory());
        } catch (Throwable ignored) {
            // fallback below
        }
        BufferPoolMXBean pool = getDirectPool();
        return pool == null ? 0L : Math.max(0L, pool.getMemoryUsed());
    }

    public static long maxDirectMemory() {
        try {
            long max = PlatformDependent.maxDirectMemory();
            if (max > 0) {
                return max;
            }
        } catch (Throwable ignored) {
            // fallback below
        }
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMXBean.getInputArguments()) {
            if (arg.startsWith("-XX:MaxDirectMemorySize=")) {
                String value = arg.substring("-XX:MaxDirectMemorySize=".length());
                return parseMemory(value);
            }
        }
        return Runtime.getRuntime().maxMemory();
    }

    private static BufferPoolMXBean getDirectPool() {
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if (DIRECT.equalsIgnoreCase(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    private static long parseMemory(String value) {
        value = value.trim().toLowerCase();
        long multiplier = 1;
        if (value.endsWith("gb")) {
            multiplier = 1024L * 1024L * 1024L;
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith("mb")) {
            multiplier = 1024L * 1024L;
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith("kb")) {
            multiplier = 1024L;
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith("g")) {
            multiplier = 1024L * 1024L * 1024L;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 1024L * 1024L;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("k")) {
            multiplier = 1024L;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("b")) {
            value = value.substring(0, value.length() - 1);
        }
        return Long.parseLong(value.trim()) * multiplier;
    }
}
