package com.xiaoniucode.etp.server.web.dto.ssl;

import lombok.Data;

import java.io.Serializable;

@Data
public class SslCertAutoRenewResultDTO implements Serializable {
    private Boolean autoRenew;
    /**
     * 本次操作是否顺带启用了 ACME 续签计划任务
     */
    private Boolean acmeRenewJobAutoEnabled;
}
