package com.xiaoniucode.etp.server.web.dto.dns;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DnsCredentialDTO implements Serializable {
    private Long id;
    private String name;
    private Integer provider;
    private String providerLabel;
    private Integer status;
    private String accountHint;
    private LocalDateTime lastTestAt;
    private String lastTestMessage;
    private LocalDateTime createdAt;
}
