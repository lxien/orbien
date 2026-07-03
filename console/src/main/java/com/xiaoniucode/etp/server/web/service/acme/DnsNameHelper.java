package com.xiaoniucode.etp.server.web.service.acme;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class DnsNameHelper {

    private DnsNameHelper() {
    }

    public static String normalize(String domain) {
        String value = domain.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("*.")) {
            value = value.substring(2);
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String buildTxtRecordName(String authDomain) {
        return "_acme-challenge." + normalize(authDomain);
    }

    /**
     * 根据域名推断 DNS 区域（用于手动模式展示，无 API 时作为兜底）。
     */
    public static String guessZone(String fqdn) {
        String normalized = normalize(fqdn);
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0) {
            return normalized;
        }
        int secondLastDot = normalized.lastIndexOf('.', lastDot - 1);
        if (secondLastDot < 0) {
            return normalized;
        }
        return normalized.substring(secondLastDot + 1);
    }

    public static String buildHostRecord(String recordFqdn, String zone) {
        return splitHostRecord(recordFqdn, zone);
    }

    public static String splitHostRecord(String recordName, String zone) {
        String normalizedRecord = normalize(recordName);
        String normalizedZone = normalize(zone);
        if (!normalizedRecord.endsWith("." + normalizedZone) && !normalizedRecord.equals(normalizedZone)) {
            throw new IllegalArgumentException("记录名与DNS区域不匹配");
        }
        if (normalizedRecord.equals(normalizedZone)) {
            return "@";
        }
        return normalizedRecord.substring(0, normalizedRecord.length() - normalizedZone.length() - 1);
    }

    public static String parentZone(String fqdn) {
        String normalized = normalize(fqdn);
        int idx = normalized.indexOf('.');
        if (idx < 0) {
            return normalized;
        }
        return normalized.substring(idx + 1);
    }

    public static boolean looksLikeWildcard(String domain) {
        return StringUtils.hasText(domain) && domain.trim().startsWith("*.");
    }
}
