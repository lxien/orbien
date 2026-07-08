/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.proxy.HttpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpsProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpsProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TcpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TcpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.Socks5ProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.Socks5ProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.UdpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.UdpProxyListDTO;
import io.github.lxien.orbien.server.web.param.proxy.*;

public interface ProxyService {
    /**
     * 创建 TCP 代理
     */
    void createTcpProxy(TcpProxyCreateParam param);

    void createSocks5Proxy(Socks5ProxyCreateParam param);

    void createUdpProxy(UdpProxyCreateParam param);

    /**
     * 创建 HTTP 代理
     */
    void createHttpProxy(HttpProxyCreateParam param);

    /**
     * 创建 HTTPS 代理
     */
    void createHttpsProxy(HttpsProxyCreateParam param);

    /**
     * 更新 TCP 代理
     */
    void updateTcpProxy(TcpProxyUpdateParam param);

    void updateUdpProxy(UdpProxyUpdateParam param);

    void updateSocks5Proxy(Socks5ProxyUpdateParam param);

    /**
     * 更新 HTTP 代理
     */
    void updateHttpProxy(HttpProxyUpdateParam param);

    /**
     * 更新 HTTPS 代理
     */
    void updateHttpsProxy(HttpsProxyUpdateParam param);

    /**
     * 根据 ID 查询tcp代理
     */
    TcpProxyDetailDTO getTcpProxyById(String id);

    UdpProxyDetailDTO getUdpProxyById(String id);

    Socks5ProxyDetailDTO getSocks5ProxyById(String id);

    HttpProxyDetailDTO getHttpProxyById(String id);

    HttpsProxyDetailDTO getHttpsProxyById(String id);

    void batchDeleteProxies(ProxyBatchDeleteParam param);

    void setProxyStatus(String id, Integer status);

    PageResult<TcpProxyListDTO> findTcpProxies(PageQuery pageQuery);

    PageResult<UdpProxyListDTO> findUdpProxies(PageQuery pageQuery);

    PageResult<Socks5ProxyListDTO> findSocks5Proxies(PageQuery pageQuery);

    PageResult<HttpProxyListDTO> findHttpProxies(PageQuery pageQuery);

    PageResult<HttpsProxyListDTO> findHttpsProxies(PageQuery pageQuery);

    void saveClusterConfig(String proxyId, ProxyClusterSaveParam param);
}