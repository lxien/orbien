package com.xiaoniucode.etp.server.web.dto.acme;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AcmeOrderDTO implements Serializable {
    private Long id;
    private String orderNo;
    private Integer status;
    private String statusLabel;
    private List<String> domains = new ArrayList<>();
    private Integer validationMode;
    private Long dnsCredentialId;
    private Integer dnsProvider;
    private String certId;
    private List<Long> bindProxyDomainIds = new ArrayList<>();
    private String errorCode;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private List<AcmeDnsChallengeDTO> challenges = new ArrayList<>();
}
