package com.xiaoniucode.etp.server.web.service.dns;

import lombok.Data;

@Data
public class DnsProviderConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String secretId;
    private String secretKey;
    private String apiToken;
    private String zoneId;
}
