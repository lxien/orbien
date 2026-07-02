package com.xiaoniucode.etp.server.port;

import com.xiaoniucode.etp.core.domain.PortInterval;
import com.xiaoniucode.etp.core.enums.PortPoolType;
import com.xiaoniucode.etp.server.exceptions.EtpException;
import com.xiaoniucode.etp.server.exceptions.PortConflictException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 单协议端口池：allowed（可分配）与 allocated（已占用）。
 */
final class PortPool {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PortPool.class);

    private final PortPoolType type;
    private final BitSet allowed = new BitSet(65536);
    private final BitSet allocated = new BitSet(65536);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    PortPool(PortPoolType type) {
        this.type = type;
    }

    Integer acquire() {
        lock.writeLock().lock();
        try {
            BitSet free = freePorts();
            if (free.isEmpty()) {
                return null;
            }
            for (int i = 0; i < 20; i++) {
                int port = randomSetBit(free);
                if (port > 0 && tryBind(port)) {
                    doMarkAllocated(port);
                    return port;
                }
            }
            for (int port = free.nextSetBit(1); port > 0; port = free.nextSetBit(port + 1)) {
                if (tryBind(port)) {
                    doMarkAllocated(port);
                    return port;
                }
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void reserve(int port) {
        lock.writeLock().lock();
        try {
            assertInAllowed(port);
            if (allocated.get(port)) {
                throw new PortConflictException(port);
            }
            doMarkAllocated(port);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean release(int port) {
        lock.writeLock().lock();
        try {
            if (!allocated.get(port)) {
                return false;
            }
            allocated.clear(port);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean isAvailable(int port) {
        lock.readLock().lock();
        try {
            return allowed.get(port) && !allocated.get(port);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 推荐可用端口，仅查询不占用
     */
    List<Integer> suggestAvailable(int limit) {
        lock.readLock().lock();
        try {
            BitSet free = freePorts();
            if (free.isEmpty() || limit <= 0) {
                return List.of();
            }
            List<Integer> result = new ArrayList<>(limit);
            BitSet scratch = (BitSet) free.clone();
            int attempts = Math.min(Math.max(limit * 8, limit), scratch.cardinality());
            for (int i = 0; i < attempts && result.size() < limit; i++) {
                int port = randomSetBit(scratch);
                if (port <= 0) {
                    break;
                }
                scratch.clear(port);
                if (tryBind(port)) {
                    result.add(port);
                }
            }
            if (result.size() < limit) {
                for (int port = free.nextSetBit(1); port > 0 && result.size() < limit; port = free.nextSetBit(port + 1)) {
                    if (result.contains(port)) {
                        continue;
                    }
                    if (tryBind(port)) {
                        result.add(port);
                    }
                }
            }
            return List.copyOf(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    void markAllocated(int port) {
        lock.writeLock().lock();
        try {
            doMarkAllocated(port);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void replaceAllowed(Collection<PortInterval> intervals) {
        lock.writeLock().lock();
        try {
            allowed.clear();
            applyIntervals(intervals, true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void addAllowed(PortInterval interval) {
        lock.writeLock().lock();
        try {
            applyInterval(interval, true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void removeAllowed(PortInterval interval) {
        lock.writeLock().lock();
        try {
            validateRemovable(interval);
            applyInterval(interval, false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void validateRemovable(PortInterval interval) {
        lock.readLock().lock();
        try {
            for (int port = interval.start(); port <= interval.end(); port++) {
                if (allocated.get(port)) {
                    throw new EtpException("端口 " + port + " 已被代理占用，无法从池中移除");
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean hasAllocatedIn(PortInterval interval) {
        lock.readLock().lock();
        try {
            for (int port = interval.start(); port <= interval.end(); port++) {
                if (allocated.get(port)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    private BitSet freePorts() {
        BitSet free = (BitSet) allowed.clone();
        free.andNot(allocated);
        return free;
    }

    private void assertInAllowed(int port) {
        if (!allowed.get(port)) {
            throw new EtpException("端口 " + port + " 不在 " + type + " 端口池范围内");
        }
    }

    private void doMarkAllocated(int port) {
        allocated.set(port);
        logger.debug("{} 端口占用: {}", type, port);
    }

    private void applyIntervals(Collection<PortInterval> intervals, boolean set) {
        for (PortInterval interval : intervals) {
            applyInterval(interval, set);
        }
    }

    private void applyInterval(PortInterval interval, boolean set) {
        for (int port = interval.start(); port <= interval.end(); port++) {
            if (set) {
                allowed.set(port);
            } else {
                allowed.clear(port);
            }
        }
    }

    private static int randomSetBit(BitSet bits) {
        int count = bits.cardinality();
        if (count == 0) {
            return -1;
        }
        int pick = ThreadLocalRandom.current().nextInt(count);
        int seen = 0;
        for (int port = bits.nextSetBit(1); port > 0; port = bits.nextSetBit(port + 1)) {
            if (seen++ == pick) {
                return port;
            }
        }
        return -1;
    }

    private boolean tryBind(int port) {
        return switch (type) {
            case TCP -> tryBindTcp(port);
            case UDP -> tryBindUdp(port);
        };
    }

    private static boolean tryBindTcp(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean tryBindUdp(int port) {
        try (DatagramSocket ignored = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
