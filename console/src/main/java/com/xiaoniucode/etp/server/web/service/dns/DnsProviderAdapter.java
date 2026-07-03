package com.xiaoniucode.etp.server.web.service.dns;

import com.xiaoniucode.etp.server.web.enums.DnsProviderType;

public interface DnsProviderAdapter {

    DnsProviderType providerType();

    void testConnection(DnsProviderConfig config);

    String resolveZone(String fqdn, DnsProviderConfig config);

    DnsRecordRef addTxtRecord(String fqdn, String recordName, String value, DnsProviderConfig config);

    void removeTxtRecord(DnsRecordRef recordRef, DnsProviderConfig config);
}
