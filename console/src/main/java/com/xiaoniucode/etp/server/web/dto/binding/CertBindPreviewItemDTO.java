package com.xiaoniucode.etp.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;

@Data
public class CertBindPreviewItemDTO implements Serializable {
    private Long proxyDomainId;
    private String fullDomain;
    private String proxyId;
    private String proxyName;
    private Boolean matched;
    private String reason;
    private Boolean hasBinding;
    private String currentCertId;
}
