package io.github.lxien.orbien.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;

@Data
public class CertBindItemResultDTO implements Serializable {
    private Long proxyDomainId;
    private String fullDomain;
    private Long bindingId;
    private String status;
    private String reason;
}
