package io.github.lxien.orbien.server.web.service.acme;

import io.github.lxien.orbien.server.web.dto.acme.AcmeHttpsProxyDomainOptionDTO;
import io.github.lxien.orbien.server.web.dto.acme.AcmeHttpsProxyOptionDTO;

import java.util.List;

public interface AcmeDomainSourceService {

    List<AcmeHttpsProxyOptionDTO> listHttpsProxyOptions();

    List<AcmeHttpsProxyDomainOptionDTO> listDomainsByProxyId(String proxyId);
}
