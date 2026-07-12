/*
 *    Copyright 2026 lxien
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
package io.github.lxien.orbien.server.web.monitor;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 获取物理内存利用率、JVM内存利用率以及CPU利用率等信息
 */
public class ServerMonitor {
    /**
     * 获取物理内存信息
     */
    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    /**
     * 获取JVM内存信息
     */
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    /**
     * 获取运行时信息
     */
    private static final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    /**
     * 获取Runtime信息
     */
    private static final Runtime runtime = Runtime.getRuntime();
    /**
     * CPU计算历史值
     */
    private static final AtomicLong prevProcessCpuTimeNs = new AtomicLong(0);
    private static final AtomicLong prevUptimeMs = new AtomicLong(0);

    /**
     * 获取JVM内存信息
     */
    public static JvmMemoryDTO getJvmMemory() {
        JvmMemoryDTO jvmMemory = new JvmMemoryDTO();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long total = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        long used = heap.getUsed();
        double usage = total > 0 ? (used * 100.0) / total : 0;
        double rounded = Math.round(usage * 10) / 10.0;
        Object usageValue;
        if (rounded == Math.floor(rounded)) {
            usageValue = (int) rounded;
        } else {
            String strValue = String.valueOf(rounded);
            if (strValue.endsWith(".0")) {
                usageValue = (int) rounded;
            } else {
                usageValue = rounded;
            }
        }
        jvmMemory.setTotal(ByteUtils.formatBytes(total));
        jvmMemory.setUsed(ByteUtils.formatBytes(used));
        jvmMemory.setUsage(usageValue);
        return jvmMemory;
    }

    /**
     * 获取物理内存信息
     */
    public static OsMemoryDTO getOsMemory() {
        OsMemoryDTO osMemory = new OsMemoryDTO();
        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        long used = total - free;
        double usage = total > 0 ? (used * 100.0) / total : 0;
        double rounded = Math.round(usage * 10) / 10.0;
        Object usageValue;
        if (rounded == Math.floor(rounded)) {
            usageValue = (int) rounded;
        } else {
            String strValue = String.valueOf(rounded);
            if (strValue.endsWith(".0")) {
                usageValue = (int) rounded;
            } else {
                usageValue = rounded;
            }
        }
        osMemory.setTotal(ByteUtils.formatBytes(total));
        osMemory.setUsed(ByteUtils.formatBytes(used));
        osMemory.setUsage(usageValue);
        return osMemory;
    }

    public static CpuDTO getCpu() {
        CpuDTO cpu = new CpuDTO();
        int cores = runtime.availableProcessors();
        long currentCpuTimeNs = osBean.getProcessCpuTime();
        long currentUptimeMs = runtimeMxBean.getUptime();
        double usage = 0.0;
        long prevCpu = prevProcessCpuTimeNs.get();
        long prevUp = prevUptimeMs.get();
        if (prevCpu != 0 && prevUp != 0 && currentUptimeMs > prevUp) {
            long uptimeDiffMs = currentUptimeMs - prevUp;
            long cpuTimeDiffNs = currentCpuTimeNs - prevCpu;
            double uptimeDiffSec = uptimeDiffMs / 1000.0;
            double cpuTimeDiffSec = cpuTimeDiffNs / 1_000_000_000.0;
            usage = (cpuTimeDiffSec / (uptimeDiffSec * cores)) * 100.0;
            usage = Math.max(0.0, Math.min(100.0, usage));
        }
        prevProcessCpuTimeNs.set(currentCpuTimeNs);
        prevUptimeMs.set(currentUptimeMs);
        double rounded = Math.round(usage * 10) / 10.0;
        Object usageValue;
        if (rounded == Math.floor(rounded)) {
            usageValue = (int) rounded;
        } else {
            String strValue = String.valueOf(rounded);
            if (strValue.endsWith(".0")) {
                usageValue = (int) rounded;
            } else {
                usageValue = rounded;
            }
        }
        cpu.setTotal(cores);
        cpu.setUsed(usageValue);
        cpu.setUsage(usageValue);
        return cpu;
    }

    public static DirectMemoryDTO getDirectMemory() {
        DirectMemoryDTO directMemory = new DirectMemoryDTO();
        long total = DirectMemoryUtils.maxDirectMemory();
        long used = DirectMemoryUtils.usedDirectMemory();
        double usage = total > 0 ? (used * 100.0) / total : 0;
        double rounded = Math.round(usage * 10) / 10.0;
        Object usageValue;
        if (rounded == Math.floor(rounded)) {
            usageValue = (int) rounded;
        } else {
            String strValue = String.valueOf(rounded);
            if (strValue.endsWith(".0")) {
                usageValue = (int) rounded;
            } else {
                usageValue = rounded;
            }
        }
        directMemory.setTotal(ByteUtils.formatBytes(total));
        directMemory.setUsed(ByteUtils.formatBytes(used));
        directMemory.setUsage(usageValue);
        return directMemory;
    }

    public static ServerDTO getServerInfo() {
        ServerDTO serverDTO = new ServerDTO();
        serverDTO.setCpu(getCpu());
        serverDTO.setJvmMem(getJvmMemory());
        serverDTO.setOsMem(getOsMemory());
        serverDTO.setDirectMem(getDirectMemory());
        return serverDTO;
    }
}
