/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package io.github.lxien.orbien.server.uid.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * DateUtils provides date formatting, parsing
 *
 * @author yutianbao
 */
public abstract class DateUtils {
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /**
     * Patterns
     */
    public static final String DAY_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern(DAY_PATTERN);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final DateTimeFormatter DATETIME_MS_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_MS_PATTERN);

    public static final Date DEFAULT_DATE = DateUtils.parseByDayPattern("1970-01-01");

    private DateUtils() {
    }

    /**
     * Parse date by 'yyyy-MM-dd' pattern
     */
    public static Date parseByDayPattern(String str) {
        return parseDate(str, DAY_PATTERN);
    }

    /**
     * Parse date by 'yyyy-MM-dd HH:mm:ss' pattern
     */
    public static Date parseByDateTimePattern(String str) {
        return parseDate(str, DATETIME_PATTERN);
    }

    /**
     * Parse date without checked exception
     */
    public static Date parseDate(String str, String pattern) {
        try {
            if (DAY_PATTERN.equals(pattern)) {
                LocalDate localDate = LocalDate.parse(str, DAY_FORMATTER);
                return Date.from(localDate.atStartOfDay(DEFAULT_ZONE).toInstant());
            }
            if (DATETIME_PATTERN.equals(pattern)) {
                LocalDateTime dateTime = LocalDateTime.parse(str, DATETIME_FORMATTER);
                return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
            }
            if (DATETIME_MS_PATTERN.equals(pattern)) {
                LocalDateTime dateTime = LocalDateTime.parse(str, DATETIME_MS_FORMATTER);
                return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            if (pattern.contains("H") || pattern.contains("m") || pattern.contains("s")) {
                LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
                return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
            }
            LocalDate localDate = LocalDate.parse(str, formatter);
            return Date.from(localDate.atStartOfDay(DEFAULT_ZONE).toInstant());
        } catch (DateTimeParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Format date into string
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        if (DAY_PATTERN.equals(pattern)) {
            return DAY_FORMATTER.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDate());
        }
        if (DATETIME_PATTERN.equals(pattern)) {
            return DATETIME_FORMATTER.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime());
        }
        if (DATETIME_MS_PATTERN.equals(pattern)) {
            return DATETIME_MS_FORMATTER.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime());
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (pattern.contains("H") || pattern.contains("m") || pattern.contains("s")) {
            return formatter.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime());
        }
        return formatter.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDate());
    }

    /**
     * Format date by 'yyyy-MM-dd' pattern
     */
    public static String formatByDayPattern(Date date) {
        return formatDate(date, DAY_PATTERN);
    }

    /**
     * Format date by 'yyyy-MM-dd HH:mm:ss' pattern
     */
    public static String formatByDateTimePattern(Date date) {
        return formatDate(date, DATETIME_PATTERN);
    }

    /**
     * Get current day using format date by 'yyyy-MM-dd' pattern
     */
    public static String getCurrentDayByDayPattern() {
        Calendar cal = Calendar.getInstance();
        return formatByDayPattern(cal.getTime());
    }

}
