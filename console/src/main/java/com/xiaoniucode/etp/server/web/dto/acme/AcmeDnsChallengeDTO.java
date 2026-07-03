package com.xiaoniucode.etp.server.web.dto.acme;

import lombok.Data;

import java.io.Serializable;

@Data
public class AcmeDnsChallengeDTO implements Serializable {
    private Long id;
    private String domain;
    private String recordName;
    /**
     * 相对主机记录，如 _acme-challenge.dd（在 DNS 控制台填写）
     */
    private String hostRecord;
    /**
     * DNS 区域，如 baidu.com
     */
    private String dnsZone;
    private String recordValue;
    private String recordType;
    private Integer status;
    private String statusLabel;
}
