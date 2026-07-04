package io.github.lxien.orbien.server.web.service.dns;

import io.github.lxien.orbien.server.web.enums.DnsProviderType;

public interface DnsProviderAdapter {

    DnsProviderType providerType();

    void testConnection(DnsProviderConfig config);

    String resolveZone(String fqdn, DnsProviderConfig config);

    DnsRecordRef addTxtRecord(String fqdn, String recordName, String value, DnsProviderConfig config);

    void removeTxtRecord(DnsRecordRef recordRef, DnsProviderConfig config);
}
