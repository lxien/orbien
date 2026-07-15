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
package io.github.lxien.orbien.server.security;

import io.github.lxien.orbien.core.domain.TimeAccessConfig;
import io.github.lxien.orbien.core.domain.TimeAccessWindow;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.time.TimeAccessSupport;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间周期访问判定；按 proxyId 缓存编译后的日程，热路径仅做整数比较
 * <p>
 * invalidate 会递增 generation，强制 Keep-Alive / 长连接上的 Handler 重新进入判定
 */
@Component
public class TimeAccessChecker {
    private static final long DISABLED_RECHECK_INTERVAL_MS = 5_000L;

    private final Map<String, CompiledSchedule> cache = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private final Clock clock;

    public TimeAccessChecker() {
        this(Clock.systemUTC());
    }

    public TimeAccessChecker(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public long generation() {
        return generation.get();
    }

    public boolean checkAccess(String proxyId, TimeAccessConfig config) {
        if (proxyId == null) {
            return false;
        }
        if (config == null || !config.isEnabled()) {
            cache.remove(proxyId);
            return true;
        }
        return resolveSchedule(proxyId, config).isAllowed(clock.instant());
    }

    /**
     * @return 下次建议再检的绝对时间戳（epoch millis）
     */
    public long nextCheckAtMillis(String proxyId, TimeAccessConfig config) {
        if (config == null || !config.isEnabled()) {
            // generation 递增仍可立即触发复检；此处短间隔是兜底
            return clock.millis() + DISABLED_RECHECK_INTERVAL_MS;
        }
        return resolveSchedule(proxyId, config).nextBoundaryEpochMillis(clock.instant());
    }

    public void invalidate(String proxyId) {
        if (proxyId != null) {
            cache.remove(proxyId);
        }
        generation.incrementAndGet();
    }

    public void invalidateAll() {
        cache.clear();
        generation.incrementAndGet();
    }

    private CompiledSchedule resolveSchedule(String proxyId, TimeAccessConfig config) {
        int fingerprint = CompiledSchedule.fingerprint(config);
        CompiledSchedule cached = cache.get(proxyId);
        if (cached != null && cached.fingerprint == fingerprint) {
            return cached;
        }
        CompiledSchedule compiled = CompiledSchedule.compile(config, fingerprint);
        cache.put(proxyId, compiled);
        return compiled;
    }

    static final class CompiledSchedule {
        private final int fingerprint;
        private final ZoneId zoneId;
        private final boolean allowMode;
        private final boolean timeEnabled;
        private final int daysMask;
        private final int[] windowStarts;
        private final int[] windowEnds;

        private CompiledSchedule(int fingerprint, ZoneId zoneId, boolean allowMode, boolean timeEnabled,
                                 int daysMask, int[] windowStarts, int[] windowEnds) {
            this.fingerprint = fingerprint;
            this.zoneId = zoneId;
            this.allowMode = allowMode;
            this.timeEnabled = timeEnabled;
            this.daysMask = daysMask;
            this.windowStarts = windowStarts;
            this.windowEnds = windowEnds;
        }

        static int fingerprint(TimeAccessConfig config) {
            AccessControl mode = config.getMode() == null ? AccessControl.ALLOW : config.getMode();
            StringBuilder windowsKey = new StringBuilder();
            for (TimeAccessWindow window : config.getWindowsView()) {
                windowsKey.append(window.getStart()).append('-').append(window.getEnd()).append(';');
            }
            return Objects.hash(
                    mode.getCode(),
                    config.isTimeEnabled(),
                    config.getTimezone(),
                    TimeAccessSupport.toDaysMask(config.getDaysView()),
                    windowsKey.toString()
            );
        }

        static CompiledSchedule compile(TimeAccessConfig config, int fingerprint) {
            AccessControl mode = config.getMode() == null ? AccessControl.ALLOW : config.getMode();
            ZoneId zone = TimeAccessSupport.resolveZoneId(config.getTimezone());
            int daysMask = TimeAccessSupport.toDaysMask(config.getDaysView());
            List<int[]> ranges = TimeAccessSupport.expandWindows(config.getWindowsView());
            int[] starts = new int[ranges.size()];
            int[] ends = new int[ranges.size()];
            for (int i = 0; i < ranges.size(); i++) {
                starts[i] = ranges.get(i)[0];
                ends[i] = ranges.get(i)[1];
            }
            return new CompiledSchedule(fingerprint, zone, mode.isAllowMode(), config.isTimeEnabled(),
                    daysMask, starts, ends);
        }

        boolean isAllowed(Instant instant) {
            ZonedDateTime zdt = instant.atZone(zoneId);
            boolean inWindow = isInWindow(zdt);
            return allowMode ? inWindow : !inWindow;
        }

        private boolean isInWindow(ZonedDateTime zdt) {
            DayOfWeek dayOfWeek = zdt.getDayOfWeek();
            if (!TimeAccessSupport.isDaySelected(daysMask, dayOfWeek)) {
                return false;
            }
            if (!timeEnabled) {
                return true;
            }
            int sod = zdt.toLocalTime().toSecondOfDay();
            for (int i = 0; i < windowStarts.length; i++) {
                if (sod >= windowStarts[i] && sod < windowEnds[i]) {
                    return true;
                }
            }
            return false;
        }

        long nextBoundaryEpochMillis(Instant instant) {
            ZonedDateTime zdt = instant.atZone(zoneId);
            int sod = zdt.toLocalTime().toSecondOfDay();
            int bestDelta = TimeAccessSupport.SECONDS_PER_DAY - sod;
            if (timeEnabled) {
                for (int end : windowEnds) {
                    if (end > sod) {
                        bestDelta = Math.min(bestDelta, end - sod);
                    }
                }
                for (int start : windowStarts) {
                    if (start > sod) {
                        bestDelta = Math.min(bestDelta, start - sod);
                    }
                }
            }
            int clamp = Math.max(1, Math.min(bestDelta, 60));
            return instant.plusSeconds(clamp).toEpochMilli();
        }
    }
}
