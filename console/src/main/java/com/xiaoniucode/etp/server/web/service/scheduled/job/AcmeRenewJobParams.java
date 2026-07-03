package com.xiaoniucode.etp.server.web.service.scheduled.job;

import lombok.Data;

@Data
public class AcmeRenewJobParams {
    private int renewBeforeDays = 30;
    private boolean onlyAcmeSource = true;
    private boolean respectCertAutoRenew = true;
    private int maxCertsPerRun = 20;
}
