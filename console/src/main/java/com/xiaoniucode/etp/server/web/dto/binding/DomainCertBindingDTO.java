package com.xiaoniucode.etp.server.web.dto.binding;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class DomainCertBindingDTO implements Serializable {
    private Long bindingId;
    private String certId;
    private List<String> certSanDomains;
    private String issuer;
    private String org;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate notAfter;
    private Integer bindStatus;
    private Boolean enabled;
}
