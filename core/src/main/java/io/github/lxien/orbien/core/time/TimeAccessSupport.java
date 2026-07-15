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
package io.github.lxien.orbien.core.time;

import io.github.lxien.orbien.core.domain.TimeAccessConfig;
import io.github.lxien.orbien.core.domain.TimeAccessWindow;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.utils.StringUtils;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验、位图编解码、时区解析与判定
 */
public final class TimeAccessSupport {
    public static final int MAX_WINDOWS = 16;
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    public static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "^(?:([01]\\d|2[0-3]):([0-5]\\d)(?::([0-5]\\d))?|24:00(?::00)?)$");

    private TimeAccessSupport() {
    }

    public static void validateConfig(TimeAccessConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("time_access 配置不能为空");
        }
        if (config.getMode() == null) {
            throw new IllegalArgumentException("time_access.mode 不能为空");
        }
        validateTimezone(config.getTimezone());
        validateDays(config.getDaysView());
        if (config.isTimeEnabled()) {
            validateWindows(config.getWindowsView());
        }
    }

    public static void validateTimezone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return;
        }
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("非法时区: " + timezone);
        }
    }

    public static ZoneId resolveZoneId(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("非法时区: " + timezone);
        }
    }

    public static void validateDays(Set<Integer> days) {
        if (days == null) {
            return;
        }
        for (Integer day : days) {
            if (day == null || day < 1 || day > 7) {
                throw new IllegalArgumentException("星期必须为 1~7（周一~周日）: " + day);
            }
        }
    }

    public static void validateWindows(List<TimeAccessWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            return;
        }
        if (windows.size() > MAX_WINDOWS) {
            throw new IllegalArgumentException("时间窗数量超过限制: " + MAX_WINDOWS);
        }
        for (TimeAccessWindow window : windows) {
            validateWindow(window);
        }
    }

    public static void validateWindow(TimeAccessWindow window) {
        if (window == null) {
            throw new IllegalArgumentException("时间窗不能为空");
        }
        int start = parseToSecondOfDay(window.getStart());
        int end = parseToSecondOfDay(window.getEnd());
        if (start == end) {
            throw new IllegalArgumentException("开始时间与结束时间不能相同");
        }
        window.setStart(normalizeTime(window.getStart()));
        window.setEnd(normalizeTime(window.getEnd()));
    }

    /**
     * 将可能跨午夜的时间窗拆成不跨日的多段，便于热路径做简单区间比较
     */
    public static List<int[]> expandWindows(List<TimeAccessWindow> windows) {
        List<int[]> result = new ArrayList<>();
        if (windows == null || windows.isEmpty()) {
            return result;
        }
        for (TimeAccessWindow window : windows) {
            int start = parseToSecondOfDay(window.getStart());
            int end = parseToSecondOfDay(window.getEnd());
            if (start < end) {
                result.add(new int[]{start, end});
            } else {
                // overnight: [start, 86400) U [0, end)
                result.add(new int[]{start, SECONDS_PER_DAY});
                if (end > 0) {
                    result.add(new int[]{0, end});
                }
            }
        }
        return result;
    }

    public static int parseToSecondOfDay(String time) {
        if (!StringUtils.hasText(time)) {
            throw new IllegalArgumentException("时间不能为空");
        }
        String trimmed = time.trim();
        if (!TIME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("非法时间格式: " + time + "，期望 HH:mm 或 HH:mm:ss");
        }
        if (trimmed.startsWith("24:00")) {
            return SECONDS_PER_DAY;
        }
        String[] parts = trimmed.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return hour * 3600 + minute * 60 + second;
    }

    public static String normalizeTime(String time) {
        int sod = parseToSecondOfDay(time);
        if (sod == SECONDS_PER_DAY) {
            return "24:00:00";
        }
        return LocalTime.ofSecondOfDay(sod).format(TIME_FORMAT);
    }

    public static int toDaysMask(Set<Integer> days) {
        int mask = 0;
        if (days == null) {
            return mask;
        }
        for (Integer day : days) {
            if (day != null && day >= 1 && day <= 7) {
                mask |= (1 << (day - 1));
            }
        }
        return mask;
    }

    public static Set<Integer> fromDaysMask(int mask) {
        Set<Integer> days = new LinkedHashSet<>();
        for (int day = 1; day <= 7; day++) {
            if ((mask & (1 << (day - 1))) != 0) {
                days.add(day);
            }
        }
        return days;
    }

    public static boolean isDaySelected(int daysMask, DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return false;
        }
        // daysMask == 0：未选任何星期，语义为「每天」
        if (daysMask == 0) {
            return true;
        }
        return (daysMask & (1 << (dayOfWeek.getValue() - 1))) != 0;
    }

    public static boolean matchAnyWindow(List<int[]> ranges, int secondOfDay) {
        if (ranges == null || ranges.isEmpty()) {
            return false;
        }
        for (int[] range : ranges) {
            if (secondOfDay >= range[0] && secondOfDay < range[1]) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllowed(TimeAccessConfig config, Instant now) {
        if (config == null || !config.isEnabled()) {
            return true;
        }
        AccessControl mode = config.getMode() != null ? config.getMode() : AccessControl.ALLOW;
        int daysMask = toDaysMask(config.getDaysView());
        ZoneId zone = resolveZoneId(config.getTimezone());
        ZonedDateTime zdt = Objects.requireNonNullElse(now, Instant.now()).atZone(zone);
        boolean inSelectedDay = isDaySelected(daysMask, zdt.getDayOfWeek());
        boolean inWindow;
        if (!config.isTimeEnabled()) {
            inWindow = inSelectedDay;
        } else {
            List<int[]> ranges = expandWindows(config.getWindowsView());
            inWindow = inSelectedDay && matchAnyWindow(ranges, zdt.toLocalTime().toSecondOfDay());
        }
        if (mode.isAllowMode()) {
            return inWindow;
        }
        return !inWindow;
    }
}
