package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.service.scheduled.job.AcmeRenewJobParams;

public interface AcmeRenewService {

    int renewDueCertificates(AcmeRenewJobParams params);
}
