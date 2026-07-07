package io.github.lxien.orbien.server.web.dto.tls;

import lombok.Data;

import java.io.Serializable;

@Data
public class TlsCertAutoRenewResultDTO implements Serializable {
    private Boolean autoRenew;
    /**
     * 本次操作是否顺带启用了 ACME 续签计划任务
     */
    private Boolean acmeRenewJobAutoEnabled;
}
