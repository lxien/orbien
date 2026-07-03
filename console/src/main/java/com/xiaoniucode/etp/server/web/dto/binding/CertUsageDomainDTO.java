package com.xiaoniucode.etp.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;

@Data
public class CertUsageDomainDTO implements Serializable {
    private Long bindingId;
    private Long proxyDomainId;
    private String fullDomain;
    private String proxyId;
    private Integer bindStatus;
    private Boolean enabled;
}
