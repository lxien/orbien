package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.dto.dns.DnsCredentialDTO;
import com.xiaoniucode.etp.server.web.dto.dns.DnsProviderSchemaDTO;
import com.xiaoniucode.etp.server.web.param.dns.DnsCredentialSaveParam;
import com.xiaoniucode.etp.server.web.service.dns.DnsProviderConfig;

import java.util.List;

public interface DnsCredentialService {

    List<DnsCredentialDTO> listAll();

    List<DnsProviderSchemaDTO> listProviderSchemas();

    DnsCredentialDTO save(DnsCredentialSaveParam param);

    void delete(Long id);

    void test(Long id);

    DnsProviderConfig resolveConfig(Long id);
}
