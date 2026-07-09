package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.transport.ProxyTransportDetailDTO;
import io.github.lxien.orbien.server.web.param.transport.TransportSaveParam;

/**
 * 代理级传输配置
 */
public interface ProxyTransportService {

    ProxyTransportDetailDTO getByProxyId(String proxyId);

    void save(String proxyId, TransportSaveParam param);
}
