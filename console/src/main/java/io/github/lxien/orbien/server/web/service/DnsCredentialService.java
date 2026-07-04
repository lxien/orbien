package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.dns.DnsCredentialDTO;
import io.github.lxien.orbien.server.web.dto.dns.DnsProviderSchemaDTO;
import io.github.lxien.orbien.server.web.param.dns.DnsCredentialSaveParam;
import io.github.lxien.orbien.server.web.service.dns.DnsProviderConfig;

import java.util.List;

public interface DnsCredentialService {

    List<DnsCredentialDTO> listAll();

    List<DnsProviderSchemaDTO> listProviderSchemas();

    DnsCredentialDTO save(DnsCredentialSaveParam param);

    void delete(Long id);

    void test(Long id);

    DnsProviderConfig resolveConfig(Long id);
}
