package com.xiaoniucode.etp.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CertUsageDTO implements Serializable {
    private String certId;
    private long usageCount;
    private List<CertUsageDomainDTO> domains = new ArrayList<>();
}
