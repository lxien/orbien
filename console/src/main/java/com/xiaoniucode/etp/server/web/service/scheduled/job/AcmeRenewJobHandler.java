package com.xiaoniucode.etp.server.web.service.scheduled.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoniucode.etp.server.web.enums.ScheduledJobCode;
import com.xiaoniucode.etp.server.web.service.AcmeRenewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcmeRenewJobHandler implements ScheduledJobHandler {

    private final AcmeRenewService acmeRenewService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledJobCode jobCode() {
        return ScheduledJobCode.ACME_RENEW;
    }

    @Override
    public JobResult execute(String paramsJson) {
        AcmeRenewJobParams params = parseParams(paramsJson);
        int renewed = acmeRenewService.renewDueCertificates(params);
        return JobResult.success(renewed, "已提交 " + renewed + " 个证书续签");
    }

    private AcmeRenewJobParams parseParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.isBlank()) {
                return new AcmeRenewJobParams();
            }
            return objectMapper.readValue(paramsJson, AcmeRenewJobParams.class);
        } catch (Exception e) {
            return new AcmeRenewJobParams();
        }
    }
}
