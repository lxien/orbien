package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.service.scheduled.job.AcmeRenewJobParams;

public interface AcmeRenewService {

    int renewDueCertificates(AcmeRenewJobParams params);
}
