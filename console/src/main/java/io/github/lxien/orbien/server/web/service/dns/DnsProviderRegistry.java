package io.github.lxien.orbien.server.web.service.dns;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.enums.DnsProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DnsProviderRegistry {

    private final Map<DnsProviderType, DnsProviderAdapter> adapters = new EnumMap<>(DnsProviderType.class);

    public DnsProviderRegistry(List<DnsProviderAdapter> adapterList) {
        for (DnsProviderAdapter adapter : adapterList) {
            adapters.put(adapter.providerType(), adapter);
        }
    }

    public DnsProviderAdapter get(DnsProviderType type) {
        DnsProviderAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new BizException("不支持的DNS厂商: " + type);
        }
        return adapter;
    }
}
