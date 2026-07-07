package io.github.lxien.orbien.server.web.service.inspector;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordDTO;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordSummaryDTO;
import io.github.lxien.orbien.server.web.dto.inspector.InspectorConfigDTO;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import io.github.lxien.orbien.server.web.param.inspector.InspectorConfigUpdateParam;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectorService {
    private final InspectorBuffer inspectorBuffer;
    private final InspectorRecordConverter recordConverter;
    private final ProxyRepository proxyRepository;
    private final ProxyConfigService proxyConfigService;
    private final InspectorProperties inspectorProperties;

    public List<HttpCaptureRecordSummaryDTO> listRequests(String proxyId, int limit) {
        validateHttpProxy(proxyId);
        int normalizedLimit = inspectorProperties.clampListLimit(limit);
        return inspectorBuffer.listSummaries(proxyId, normalizedLimit).stream()
                .map(recordConverter::toSummaryDto)
                .toList();
    }

    public HttpCaptureRecordDTO getRequest(String id) {
        HttpCaptureRecord record = inspectorBuffer.findById(id);
        if (record == null) {
            throw new BizException("请求记录不存在");
        }
        return recordConverter.toDetailDto(record);
    }

    public void clearRequests(String proxyId) {
        validateHttpProxy(proxyId);
        inspectorBuffer.clear(proxyId);
    }

    public InspectorConfigDTO getConfig(String proxyId) {
        ProxyDO proxy = requireHttpProxy(proxyId);
        InspectorConfigDTO dto = new InspectorConfigDTO();
        dto.setProxyId(proxy.getId());
        dto.setInspectorEnabled(Boolean.TRUE.equals(proxy.getInspectorEnabled()));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public InspectorConfigDTO updateConfig(InspectorConfigUpdateParam param) {
        ProxyDO proxy = requireHttpProxy(param.getProxyId());
        proxy.setInspectorEnabled(param.isInspectorEnabled());
        proxyRepository.save(proxy);
        proxyConfigService.evictByProxyId(proxy.getId());
        return getConfig(proxy.getId());
    }

    private void validateHttpProxy(String proxyId) {
        requireHttpProxy(proxyId);
    }

    private ProxyDO requireHttpProxy(String proxyId) {
        ProxyDO proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理不存在"));
        if (!ProtocolType.isHttp(proxy.getProtocol()) && !ProtocolType.isHttps(proxy.getProtocol())) {
            throw new BizException("仅 HTTP/HTTPS 代理支持流量抓包");
        }
        return proxy;
    }
}
