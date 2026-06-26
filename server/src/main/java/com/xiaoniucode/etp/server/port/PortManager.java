/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.xiaoniucode.etp.server.port;

import com.xiaoniucode.etp.server.exceptions.EtpException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PortManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(PortManager.class);

    private final BitSet allocatedPorts;
    private final int startPort;
    private final int endPort;
    private final int portCount;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public PortManager(int start, int end) {
        startPort = Math.max(start, 1);
        endPort = Math.min(end, 65535);

        if (endPort < 1 || startPort > endPort) {
            throw new IllegalArgumentException("无效的端口范围: " + startPort + "-" + endPort);
        }
        portCount = endPort - startPort + 1;
        allocatedPorts = new BitSet(portCount);

        logger.debug("端口分配器初始化，范围: {}-{}", startPort, endPort);
    }

    public Integer acquire() {
        writeLock.lock();
        try {
            int totalAllocated = allocatedPorts.cardinality();
            if (totalAllocated >= portCount) {
                throw new EtpException("无可用端口");
            }
            for (int i = 0; i < 20; i++) {
                int offset = ThreadLocalRandom.current().nextInt(portCount);
                int absPort = startPort + offset;
                if (allocatedPorts.get(offset)) continue;
                if (tryBindPort(absPort)) {
                    allocatedPorts.set(offset);
                    logger.debug("端口分配成功: {}", absPort);
                    return absPort;
                }
            }
            logger.debug("随机分配端口失败，尝试顺序查找");
            for (int offset = 0; offset < portCount; offset++) {
                if (allocatedPorts.get(offset)) continue;
                int absPort = startPort + offset;
                if (tryBindPort(absPort)) {
                    allocatedPorts.set(offset);
                    logger.debug("端口分配成功: {}", absPort);
                    return absPort;
                }
            }
            logger.debug("范围内所有端口都不可用");
            return null;
        } finally {
            writeLock.unlock();
        }

    }

    private boolean tryBindPort(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean release(int port) {
        writeLock.lock();
        try {
            if (port < startPort || port > endPort) {
                logger.debug("释放端口超出范围: {}", port);
                return false;
            }
            int offset = port - startPort;
            if (allocatedPorts.get(offset)) {
                allocatedPorts.clear(offset);
                logger.debug("释放端口占用: {}", port);
                return true;
            } else {
                logger.debug("尝试释放未分配的端口: {}", port);
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isAvailable(int port) {
        readLock.lock();
        try {
            if (port < startPort || port > endPort) {
                logger.warn("端口 {} 不在允许范围 {}-{} 内", port, startPort, endPort);
                return false;
            }
            int offset = port - startPort;
            if (allocatedPorts.get(offset)) {
                return false;
            }
            return tryBindPort(port);
        } finally {
            readLock.unlock();
        }
    }

    public void addPort(Integer port) throws EtpException {
        if (port < startPort || port > endPort) {
            logger.warn("addPort: 端口 {} 不在范围 {}-{}", port, startPort, endPort);
            throw new EtpException("端口 " + port + " 不在允许范围内");
        }
        int offset = port - startPort;
        allocatedPorts.set(offset);
    }
}