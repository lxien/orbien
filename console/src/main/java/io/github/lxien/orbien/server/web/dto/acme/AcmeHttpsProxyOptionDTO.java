package io.github.lxien.orbien.server.web.dto.acme;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AcmeHttpsProxyOptionDTO implements Serializable {
    private String proxyId;
    private String name;
    private String agentId;
    private String agentName;
    private Integer status;
    private int domainCount;
    private List<String> domainPreview = new ArrayList<>();
}
