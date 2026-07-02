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
package com.xiaoniucode.etp.core.utils;

import com.xiaoniucode.etp.core.domain.PortInterval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 端口区间工具：合并、展开、差集计算。
 */
public final class PortIntervalUtils {
    private PortIntervalUtils() {
    }

    /**
     * 合并重叠或相邻的端口区间。
     * <ol>
     *   <li>按起始端口升序排序</li>
     *   <li>线性扫描：若下一区间与当前区间重叠或相邻（next.start ≤ current.end + 1），则扩展当前右端；否则落盘当前区间并开始新区间</li>
     * </ol>
     * 时间 O(n log n)，空间 O(n)，n 为输入区间条数。
     */
    public static List<PortInterval> merge(Collection<PortInterval> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return List.of();
        }
        // 按 start 排序
        List<PortInterval> sorted = intervals.stream()
                .sorted(Comparator.comparingInt(PortInterval::start))
                .toList();

        // 单次扫描合并
        List<PortInterval> merged = new ArrayList<>();
        PortInterval current = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            PortInterval next = sorted.get(i);
            if (next.start() <= current.end() + 1) {
                // 重叠或相邻：合并为更宽区间
                current = new PortInterval(current.start(), Math.max(current.end(), next.end()));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return Collections.unmodifiableList(merged);
    }

    /**
     * 从源区间集合中扣除已占用区间，返回剩余可用区间。
     * <p>
     * 先合并源与占用区间，再逐段做区间差集。
     * 时间 O(n log n + m log m + k·s)，空间 O(n + m)，
     * n、m 为源/占用条数，k 为合并后占用条数，s 为当前可用条数（通常远小于 n）。
     */
    public static List<PortInterval> subtractAll(Collection<PortInterval> source, Collection<PortInterval> occupied) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<PortInterval> available = new ArrayList<>(merge(source));
        if (occupied == null || occupied.isEmpty()) {
            return available;
        }
        for (PortInterval block : merge(occupied)) {
            available = subtract(available, block);
            if (available.isEmpty()) {
                break;
            }
        }
        return available;
    }

    /**
     * 将区间集合展开为升序端口列表（内部先 merge 去重合并）。
     * 时间 O(n log n + P)，空间 O(P)，n 为区间条数，P 为展开后的端口总数。
     */
    public static List<Integer> toPortList(Collection<PortInterval> intervals) {
        List<PortInterval> merged = merge(intervals);
        if (merged.isEmpty()) {
            return List.of();
        }
        int total = totalCount(merged);
        List<Integer> ports = new ArrayList<>(total);
        for (PortInterval interval : merged) {
            for (int port = interval.start(); port <= interval.end(); port++) {
                ports.add(port);
            }
        }
        return Collections.unmodifiableList(ports);
    }

    /**
     * 统计区间内的端口总数。
     * 时间 O(n)，空间 O(1)，n 为区间条数。
     */
    public static int totalCount(Collection<PortInterval> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return 0;
        }
        return intervals.stream().mapToInt(PortInterval::count).sum();
    }

    private static List<PortInterval> subtract(List<PortInterval> source, PortInterval block) {
        List<PortInterval> result = new ArrayList<>();
        for (PortInterval interval : source) {
            result.addAll(subtractSingle(interval, block));
        }
        return result;
    }

    private static List<PortInterval> subtractSingle(PortInterval interval, PortInterval block) {
        if (!interval.overlaps(block)) {
            return List.of(interval);
        }
        List<PortInterval> parts = new ArrayList<>(2);
        if (block.start() > interval.start()) {
            parts.add(new PortInterval(interval.start(), block.start() - 1));
        }
        if (block.end() < interval.end()) {
            parts.add(new PortInterval(block.end() + 1, interval.end()));
        }
        return parts;
    }
}
