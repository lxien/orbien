package com.xiaoniucode.etp.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProxyCertMatrixDTO implements Serializable {
    private String proxyId;
    private int totalDomains;
    private int boundCount;
    private int unboundCount;
    private int warningCount;
    private List<ProxyDomainCertItemDTO> domains = new ArrayList<>();
}
