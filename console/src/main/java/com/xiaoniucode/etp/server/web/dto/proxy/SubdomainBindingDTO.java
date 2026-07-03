package com.xiaoniucode.etp.server.web.dto.proxy;

import lombok.Data;

@Data
public class SubdomainBindingDTO {
    /**
     * 根域名 ID
     */
    private Integer rootDomainId;
    /**
     * 子域名前缀
     */
    private String prefix;
    /**
     * 根域名
     */
    private String rootDomain;
}
