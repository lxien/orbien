package io.github.lxien.orbien.server.web.service.dns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.enums.DnsProviderType;
import io.github.lxien.orbien.server.web.service.acme.DnsNameHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class CloudflareDnsAdapter implements DnsProviderAdapter {

    private static final String API_BASE = "https://api.cloudflare.com/client/v4";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public DnsProviderType providerType() {
        return DnsProviderType.CLOUDFLARE;
    }

    @Override
    public void testConnection(DnsProviderConfig config) {
        validate(config);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/user/tokens/verify"))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .GET()
                    .build();
            JsonNode root = send(request);
            if (!root.path("success").asBoolean(false)) {
                throw new BizException("Cloudflare Token 无效");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Cloudflare 连接失败: " + e.getMessage());
        }
    }

    @Override
    public String resolveZone(String fqdn, DnsProviderConfig config) {
        return resolveZoneName(fqdn, config);
    }

    @Override
    public DnsRecordRef addTxtRecord(String fqdn, String recordName, String value, DnsProviderConfig config) {
        validate(config);
        try {
            String zoneName = resolveZoneName(fqdn, config);
            String zoneId = resolveZoneId(zoneName, config);
            String hostRecord = DnsNameHelper.splitHostRecord(recordName, zoneName);
            Map<String, Object> body = new HashMap<>();
            body.put("type", "TXT");
            body.put("name", hostRecord);
            body.put("content", value);
            body.put("ttl", 120);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/zones/" + zoneId + "/dns_records"))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            JsonNode root = send(request);
            String recordId = root.path("result").path("id").asText();
            return new DnsRecordRef(recordId, zoneId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Cloudflare 添加TXT记录失败: " + e.getMessage());
        }
    }

    @Override
    public void removeTxtRecord(DnsRecordRef recordRef, DnsProviderConfig config) {
        if (recordRef == null || !StringUtils.hasText(recordRef.providerRecordId())) {
            return;
        }
        validate(config);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/zones/" + recordRef.zone() + "/dns_records/" + recordRef.providerRecordId()))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .DELETE()
                    .build();
            send(request);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Cloudflare 删除TXT记录失败: " + e.getMessage());
        }
    }

    private String resolveZoneName(String fqdn, DnsProviderConfig config) {
        if (StringUtils.hasText(config.getZoneId())) {
            return fetchZoneName(config.getZoneId(), config);
        }
        String candidate = DnsNameHelper.normalize(fqdn);
        while (StringUtils.hasText(candidate)) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/zones?name=" + candidate))
                        .header("Authorization", "Bearer " + config.getApiToken())
                        .GET()
                        .build();
                JsonNode root = send(request);
                JsonNode result = root.path("result");
                if (result.isArray() && !result.isEmpty()) {
                    return result.get(0).path("name").asText(candidate);
                }
            } catch (Exception ignored) {
            }
            int idx = candidate.indexOf('.');
            if (idx < 0) {
                break;
            }
            candidate = candidate.substring(idx + 1);
        }
        throw new BizException("无法在 Cloudflare 找到域名区域: " + fqdn);
    }

    private String resolveZoneId(String zoneName, DnsProviderConfig config) throws Exception {
        if (StringUtils.hasText(config.getZoneId())) {
            return config.getZoneId();
        }
        return findZoneId(zoneName, config);
    }

    private String fetchZoneName(String zoneId, DnsProviderConfig config) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/zones/" + zoneId))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .GET()
                    .build();
            JsonNode root = send(request);
            return root.path("result").path("name").asText();
        } catch (Exception e) {
            throw new BizException("Cloudflare Zone 不存在: " + zoneId);
        }
    }

    private String findZoneId(String zoneName, DnsProviderConfig config) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/zones?name=" + zoneName))
                .header("Authorization", "Bearer " + config.getApiToken())
                .GET()
                .build();
        JsonNode root = send(request);
        JsonNode result = root.path("result");
        if (!result.isArray() || result.isEmpty()) {
            throw new BizException("Cloudflare 区域不存在: " + zoneName);
        }
        return result.get(0).path("id").asText();
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("success").asBoolean(false)) {
            String message = root.path("errors").isArray() && !root.path("errors").isEmpty()
                    ? root.path("errors").get(0).path("message").asText("Cloudflare API 错误")
                    : "Cloudflare API 错误";
            throw new BizException(message);
        }
        return root;
    }

    private void validate(DnsProviderConfig config) {
        if (!StringUtils.hasText(config.getApiToken())) {
            throw new BizException("Cloudflare API Token 未配置");
        }
    }
}
