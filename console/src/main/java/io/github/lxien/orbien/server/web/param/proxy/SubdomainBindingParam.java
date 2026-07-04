package io.github.lxien.orbien.server.web.param.proxy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubdomainBindingParam {
    /** 根域名 ID */
    private Integer rootDomainId;
    /** 子域名前缀 */
    private String prefix;
}
