package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.api.TransportEncryptResolver;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.TransportConfig;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.transport.ProxyTransportDetailDTO;
import io.github.lxien.orbien.server.web.dto.transport.TransportEncryptConstraints;
import io.github.lxien.orbien.server.web.dto.transport.TransportProtocolConstraints;
import io.github.lxien.orbien.server.web.dto.transport.TransportTunnelConstraints;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import io.github.lxien.orbien.server.web.param.transport.TransportSaveParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.service.ProxyTransportService;
import io.github.lxien.orbien.server.web.service.support.TransportEncryptConstraintSupport;
import io.github.lxien.orbien.server.web.service.support.TransportProtocolConstraintSupport;
import io.github.lxien.orbien.server.web.service.support.TransportTunnelConstraintSupport;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProxyTransportServiceImpl implements ProxyTransportService {

    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;
    @Autowired
    private TransactionHelper transactionHelper;
    @Resource
    private AppConfig appConfig;

    @Override
    public ProxyTransportDetailDTO getByProxyId(String proxyId) {
        ProxyDO proxyDO = loadProxy(proxyId);
        return toDetailDTO(proxyDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String proxyId, TransportSaveParam param) {
        ProxyDO proxyDO = loadProxy(proxyId);
        TransportProtocol dataProtocol = resolveParamProtocol(param.getDataProtocol());
        boolean globalTlsEnabled = isGlobalTlsEnabled();

        TransportProtocolConstraintSupport.validateAvailable(appConfig, dataProtocol);
        validateEncrypt(dataProtocol, globalTlsEnabled, param.getEncrypt());
        validateTunnelType(proxyDO.getProtocol(), dataProtocol, param.getTunnelType());

        proxyDO.setTransportProtocol(dataProtocol);
        proxyDO.setEncrypt(param.getEncrypt());
        proxyDO.setMultiplex(TransportEncryptConstraintSupport.toMultiplex(param.getTunnelType()));
        proxyRepository.save(proxyDO);

        transactionHelper.afterCommit(() -> proxyRuntimeSyncService.syncProxy(proxyId));
    }

    private ProxyTransportDetailDTO toDetailDTO(ProxyDO proxyDO) {
        TransportProtocol storedProtocol = proxyDO.getTransportProtocol();
        TransportProtocol effectiveProtocol = TransportProtocolConstraintSupport.resolveStoredProtocol(storedProtocol);
        boolean globalTlsEnabled = isGlobalTlsEnabled();
        Boolean storedEncrypt = proxyDO.getEncrypt();
        Boolean storedMultiplex = proxyDO.getMultiplex();

        boolean effectiveEncrypt = TransportEncryptConstraintSupport.resolveEffectiveEncrypt(
                effectiveProtocol, globalTlsEnabled, storedEncrypt);
        TransportEncryptConstraints encryptConstraints = TransportEncryptConstraintSupport.build(
                effectiveProtocol, globalTlsEnabled, storedEncrypt);
        TransportTunnelConstraints tunnelConstraints = TransportTunnelConstraintSupport.build(
                proxyDO.getProtocol(), effectiveProtocol, storedMultiplex);
        int effectiveTunnelType = TransportTunnelConstraintSupport.resolveEffectiveTunnelType(
                proxyDO.getProtocol(), effectiveProtocol, storedMultiplex);
        TransportProtocolConstraints protocolConstraints = TransportProtocolConstraintSupport.build(appConfig);

        ProxyTransportDetailDTO dto = new ProxyTransportDetailDTO();
        dto.setDataProtocol(effectiveProtocol.getCode());
        dto.setEffectiveDataProtocol(effectiveProtocol.getCode());
        dto.setEncrypt(storedEncrypt != null ? storedEncrypt : true);
        dto.setEffectiveEncrypt(effectiveEncrypt);
        dto.setEncryptConstraints(encryptConstraints);
        dto.setTunnelType(TransportTunnelConstraintSupport.resolveStoredTunnelType(storedMultiplex));
        dto.setEffectiveTunnelType(effectiveTunnelType);
        dto.setTunnelConstraints(tunnelConstraints);
        dto.setProtocolConstraints(protocolConstraints);
        return dto;
    }

    private void validateEncrypt(TransportProtocol dataProtocol,
                                 boolean globalTlsEnabled,
                                 Boolean encrypt) {
        if (encrypt == null) {
            throw new BizException("encrypt 不能为空");
        }
        if (TransportEncryptResolver.requiresTls(dataProtocol) && !encrypt) {
            throw new BizException("当前传输协议必须使用 TLS，无法关闭隧道加密");
        }
        if (dataProtocol == TransportProtocol.TCP && !globalTlsEnabled && encrypt) {
            throw new BizException("服务端未启用传输 TLS，无法开启隧道加密");
        }
    }

    private void validateTunnelType(ProtocolType proxyProtocol,
                                    TransportProtocol dataProtocol,
                                    Integer tunnelType) {
        TunnelType type = TunnelType.fromCode(tunnelType);
        if (type == null) {
            throw new BizException("隧道类型无效");
        }
        if (TransportTunnelConstraintSupport.requiresMultiplexOnly(proxyProtocol, dataProtocol)
                && type.isDirect()) {
            if (proxyProtocol != null && proxyProtocol.isUdp()) {
                throw new BizException("UDP 代理仅支持多路复用");
            }
            if (dataProtocol == TransportProtocol.QUIC) {
                throw new BizException("QUIC 传输协议仅支持多路复用");
            }
            throw new BizException("当前传输协议仅支持多路复用");
        }
    }

    private TransportProtocol resolveParamProtocol(Integer code) {
        TransportProtocol protocol = TransportProtocol.fromCode(code);
        if (protocol == null) {
            throw new BizException("传输协议无效");
        }
        return protocol;
    }

    private boolean isGlobalTlsEnabled() {
        TransportConfig transportConfig = appConfig.getTransportConfig();
        return transportConfig != null
                && transportConfig.getTlsConfig() != null
                && transportConfig.getTlsConfig().isEnabled();
    }

    private ProxyDO loadProxy(String proxyId) {
        return proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));
    }
}
