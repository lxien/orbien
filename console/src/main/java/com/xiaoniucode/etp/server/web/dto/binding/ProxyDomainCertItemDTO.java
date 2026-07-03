package com.xiaoniucode.etp.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProxyDomainCertItemDTO implements Serializable {
    private Long proxyDomainId;
    private String fullDomain;
    private Integer domainType;
    private DomainCertBindingDTO binding;
}
