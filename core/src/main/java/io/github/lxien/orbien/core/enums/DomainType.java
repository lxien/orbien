package io.github.lxien.orbien.core.enums;

import lombok.Getter;

@Getter
public enum DomainType {
    AUTO(0, "自动子域名"),
    SUBDOMAIN(1, "子域名"),
    CUSTOM_DOMAIN(2, "自定义域名");

    DomainType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    private final Integer code;
    private final String description;

    public static DomainType fromCode(Integer code) {
        for (DomainType domainType : values()) {
            if (domainType.getCode().equals(code)) {
                return domainType;
            }
        }
       throw new IllegalArgumentException("不合法输入");
    }
    public boolean isAuto(){
       return this==AUTO;
    }
    public boolean isSubdomain(){
        return this==SUBDOMAIN;
    }
    public boolean isCustomDomain(){
        return this==CUSTOM_DOMAIN;
    }

}
