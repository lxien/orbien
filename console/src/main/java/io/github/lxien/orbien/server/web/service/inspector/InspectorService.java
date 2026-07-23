package io.github.lxien.orbien.server.web.service.inspector;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.inspector.replay.InspectorReplayService;
import io.github.lxien.orbien.server.inspector.replay.ReplayException;
import io.github.lxien.orbien.server.inspector.replay.ReplayOptions;
import io.github.lxien.orbien.server.inspector.replay.ReplayOverrides;
import io.github.lxien.orbien.server.inspector.replay.ReplayResult;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordDTO;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordSummaryDTO;
import io.github.lxien.orbien.server.web.dto.inspector.InspectorConfigDTO;
import io.github.lxien.orbien.server.web.dto.inspector.ReplayResultDTO;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import io.github.lxien.orbien.server.web.param.inspector.InspectorConfigUpdateParam;
import io.github.lxien.orbien.server.web.param.inspector.ReplayOverridesParam;
import io.github.lxien.orbien.server.web.param.inspector.ReplayRequestParam;
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
    private final InspectorReplayService inspectorReplayService;

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

    public ReplayResultDTO replay(String id, ReplayRequestParam param) {
        ReplayRequestParam safeParam = param != null ? param : new ReplayRequestParam();
        HttpCaptureRecord source = inspectorBuffer.findById(id);
        if (source == null) {
            throw new BizException(ReplayException.CODE_NOT_FOUND, "请求记录不存在或已从缓冲中淘汰");
        }
        requireHttpProxy(source.getProxyId());

        boolean captureToBuffer = safeParam.getCaptureToBuffer() == null || safeParam.getCaptureToBuffer();
        long timeoutMs = inspectorProperties.getReplayTimeoutMs();
        if (safeParam.getTimeoutSeconds() != null && safeParam.getTimeoutSeconds() > 0) {
            timeoutMs = Math.min(safeParam.getTimeoutSeconds(), 60) * 1000L;
        }

        ReplayOptions options = ReplayOptions.builder()
                .captureToBuffer(captureToBuffer)
                .timeoutMs(timeoutMs)
                .overrides(toOverrides(safeParam.getOverrides()))
                .build();

        try {
            ReplayResult result = inspectorReplayService.replay(id, options);
            ReplayResultDTO dto = new ReplayResultDTO();
            dto.setSourceRecordId(result.getSourceRecordId());
            dto.setReplayRecordId(result.getReplayRecordId());
            dto.setModified(result.isModified());
            dto.setStatus(result.getStatus() != null ? result.getStatus().name() : null);
            dto.setRecord(recordConverter.toDetailDto(result.getRecord()));
            return dto;
        } catch (ReplayException ex) {
            throw new BizException(ex.getCode(), ex.getMessage());
        }
    }

    private static ReplayOverrides toOverrides(ReplayOverridesParam param) {
        if (param == null) {
            return null;
        }
        if (param.getMethod() == null && param.getPath() == null
                && param.getHeaders() == null && param.getBody() == null) {
            return null;
        }
        return ReplayOverrides.builder()
                .method(param.getMethod())
                .path(param.getPath())
                .headers(param.getHeaders())
                .body(param.getBody())
                .build();
    }

    private void validateHttpProxy(String proxyId) {
        requireHttpProxy(proxyId);
    }

    private ProxyDO requireHttpProxy(String proxyId) {
        ProxyDO proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理不存在"));
        if (!ProtocolType.isHttp(proxy.getProtocol()) && !ProtocolType.isHttps(proxy.getProtocol())) {
            throw new BizException("仅 HTTP/HTTPS代理支持流量抓包");
        }
        return proxy;
    }
}
