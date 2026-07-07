package io.github.lxien.orbien.server.web.dto.acme;

import lombok.Data;

import java.io.Serializable;

@Data
public class AcmeHttpsProxyDomainOptionDTO implements Serializable {
    private Long proxyDomainId;
    private String fullDomain;
    private Integer domainType;
    private String domainTypeLabel;
    private boolean selectable;
    private String unselectableReason;
    private boolean bound;
    private String boundCertIssuer;
}
