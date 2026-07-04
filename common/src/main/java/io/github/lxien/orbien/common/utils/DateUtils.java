package io.github.lxien.orbien.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DEFAULT_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    
    public static String formatTimestamp(long timestamp) {
        return formatTimestamp(timestamp, DEFAULT_FORMATTER);
    }
    
    public static String formatTimestamp(long timestamp, DateTimeFormatter formatter) {
        if (timestamp <= 0) return "N/A";
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            DEFAULT_ZONE
        );
        return dateTime.format(formatter);
    }
}