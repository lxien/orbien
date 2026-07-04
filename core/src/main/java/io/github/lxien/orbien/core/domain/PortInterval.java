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
package io.github.lxien.orbien.core.domain;

/**
 * 闭区间端口范围 [start, end]
 */
public record PortInterval(int start, int end) {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public PortInterval {
        if (start < MIN_PORT || end > MAX_PORT) {
            throw new IllegalArgumentException("端口必须在 " + MIN_PORT + "-" + MAX_PORT + " 范围内");
        }
        if (start > end) {
            throw new IllegalArgumentException("无效端口区间: " + start + "-" + end);
        }
    }

    public static PortInterval ofPort(int port) {
        return new PortInterval(port, port);
    }

    public static PortInterval ofRange(int start, Integer end) {
        return new PortInterval(start, end == null ? start : end);
    }

    public boolean isSinglePort() {
        return start == end;
    }

    public int count() {
        return end - start + 1;
    }

    public boolean overlaps(PortInterval other) {
        return start <= other.end && other.start <= end;
    }
}
