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
package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.port.PortPoolManager;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.vhost.DomainGenerator;
import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.proxy.*;
import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5UserDTO;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.param.proxy.*;
import io.github.lxien.orbien.server.web.dto.loadbalance.LoadBalanceDTO;
import io.github.lxien.orbien.server.web.proxy.service.ProxyConfigSyncService;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.*;
import io.github.lxien.orbien.server.web.repository.*;
import io.github.lxien.orbien.server.web.service.CertBindingSyncService;
import io.github.lxien.orbien.server.web.service.CertBindingService;
import io.github.lxien.orbien.server.web.service.MetricsService;
import io.github.lxien.orbien.server.web.service.ProxyService;
import io.github.lxien.orbien.server.web.service.support.TargetHealthEnricher;
import io.github.lxien.orbien.server.web.service.converter.*;
import io.github.lxien.orbien.server.web.service.converter.ProxyConvert;
import io.github.lxien.orbien.server.web.service.converter.ProxyTargetConvert;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProxyServiceImpl implements ProxyService {
    private final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private Socks5AuthRepository socks5AuthRepository;
    @Autowired
    private Socks5UserRepository socks5UserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private ProxyConvert proxyConvert;
    @Autowired
    private ProxyTargetConvert proxyTargetConvert;
    @Resource
    private AppConfig appConfig;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private DomainGenerator domainGenerator;
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private PortPoolManager portPoolManager;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ProxyConfigSyncService proxyConfigSyncService;
    @Autowired
    private CertBindingSyncService certBindingSyncService;
    @Autowired
    private CertBindingService certBindingService;
    @Autowired
    private TargetHealthEnricher targetHealthEnricher;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;
    @Autowired
    private HealthManager healthManager;
    @Autowired
    private ProxyConfigService proxyConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHttpProxy(HttpProxyCreateParam param) {
        createHttpLikeProxy(param, ProtocolType.HTTP, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHttpsProxy(HttpsProxyCreateParam param) {
        createHttpLikeProxy(param, ProtocolType.HTTPS, param.getForceHttps());
    }

    private void createHttpLikeProxy(HttpProxyCreateParam param, ProtocolType protocol, Boolean forceHttps) {
        DomainType domainType = DomainType.fromCode(param.getDomainType());
        validateHttpDomainInput(domainType, param.getSubdomainBindings(), param.getCustomDomains());

        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("名称已经存在: " + param.getName());
        }

        String proxyId = uidGenerator.getUIDAsString();
        ProxyDO proxyDO = buildHttpLikeProxyDO(param.getAgentId(), param.getName(), proxyId, domainType, protocol, forceHttps);
        applyHttpLimitTotal(proxyDO, param.getLimitTotal());
        proxyRepository.save(proxyDO);

        proxyTargetRepository.save(buildHttpTarget(param.getLocalHost(), param.getLocalPort(), param.getName(), proxyId));
        saveHttpDomains(proxyId, domainType, param.getSubdomainBindings(), param.getCustomDomains(), null);

        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        basicAuthRepository.save(new BasicAuthDO(proxyId, false));
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, HealthCheckType.HTTP));

        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, true));
        logger.debug("{}代理创建成功：{}", protocol.name(), proxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHttpProxy(HttpProxyUpdateParam param) {
        updateHttpLikeProxy(param, ProtocolType.HTTP, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHttpsProxy(HttpsProxyUpdateParam param) {
        updateHttpLikeProxy(param, ProtocolType.HTTPS, param.getForceHttps());
    }

    private void updateHttpLikeProxy(HttpProxyUpdateParam param, ProtocolType protocol, Boolean forceHttps) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));
        assertHttpLikeProtocol(existsProxyDO, protocol);

        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("代理名称已经存在: " + param.getName());
        }

        DomainType existsDomainType = existsProxyDO.getDomainType();
        DomainType requestDomainType = DomainType.fromCode(param.getDomainType());
        validateHttpDomainInput(requestDomainType, param.getSubdomainBindings(), param.getCustomDomains());

        existsProxyDO.setName(param.getName());
        existsProxyDO.setDomainType(requestDomainType);
        applyHttpLimitTotal(existsProxyDO, param.getLimitTotal());
        if (protocol.isHttps()) {
            existsProxyDO.setForceHttps(forceHttps == null || Boolean.TRUE.equals(forceHttps));
        }
        proxyRepository.save(existsProxyDO);

        replaceSingleTargetIfNotCluster(proxyId, () ->
                proxyTargetRepository.save(buildHttpTarget(param.getLocalHost(), param.getLocalPort(), param.getName(), proxyId)));

        if (requestDomainType.isAuto() && existsDomainType.isAuto()) {
            // 自动域名类型未变化，保留已有域名
        } else {
            if (protocol.isHttps()) {
                certBindingSyncService.removeBindingsByProxyId(proxyId);
            }
            proxyDomainRepository.deleteByProxyId(proxyId);
            saveHttpDomains(proxyId, requestDomainType, param.getSubdomainBindings(), param.getCustomDomains(), proxyId);
        }

        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, false));
        logger.debug("{}代理更新成功：{}", protocol.name(), existsProxyDO.getName());
    }

    /**
     * 查询 HTTP 代理列表
     *
     * @param pageQuery 分页查询参数
     * @return HTTP 代理列表
     */
    @Override
    public PageResult<HttpProxyListDTO> findHttpProxies(PageQuery pageQuery) {
        return findHttpLikeProxies(pageQuery, ProtocolType.HTTP, appConfig.getHttpProxyPort());
    }

    @Override
    public PageResult<HttpsProxyListDTO> findHttpsProxies(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());
        int httpsProxyPort = appConfig.getHttpsProxyPort();

        Page<ProxyListQueryResult> resultPage = proxyRepository.findProxiesWithAssociations(ProtocolType.HTTPS, pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }

        List<ProxyListQueryResult> content = resultPage.getContent();
        List<String> proxyIds = content.stream()
                .map(ProxyListQueryResult::getProxyDO)
                .map(ProxyDO::getId)
                .collect(Collectors.toList());

        Map<String, List<String>> domainsMap = proxyDomainRepository.findByProxyIdIn(proxyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ProxyDomainDO::getProxyId,
                        Collectors.mapping(ProxyDomainDO::getFullDomain, Collectors.toList())
                ));
        Map<String, List<ProxyTargetDO>> targetsMap = proxyTargetRepository.findByProxyIdIn(proxyIds)
                .stream()
                .collect(Collectors.groupingBy(ProxyTargetDO::getProxyId));
        Map<String, TlsCertSummaryDTO> tlsCertSummaryMap = certBindingService.summarizeTlsCertByProxyIds(proxyIds);

        Map<String, List<TargetDTO>> targetDtoMap = new HashMap<>();
        List<HttpsProxyListDTO> res = new ArrayList<>();
        for (ProxyListQueryResult r : content) {
            ProxyDO proxyDO = r.getProxyDO();
            AgentDO agentDO = r.getAgentDO();
            HttpsProxyListDTO httpsDTO = proxyConvert.toHttpsListDTO(proxyDO, httpsProxyPort, agentDO);

            httpsDTO.setDomains(domainsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            List<TargetDTO> targets = proxyTargetConvert.toDTOList(
                    targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            targetDtoMap.put(proxyDO.getId(), targets);
            httpsDTO.setTargets(targets);
            proxyConvert.enrichTlsCertSummary(httpsDTO, tlsCertSummaryMap.get(proxyDO.getId()));
            res.add(httpsDTO);
        }
        targetHealthEnricher.enrichBatch(targetDtoMap);
        return PageResult.wrap(resultPage, res);
    }

    private PageResult<HttpProxyListDTO> findHttpLikeProxies(PageQuery pageQuery, ProtocolType protocolType, int proxyPort) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());

        Page<ProxyListQueryResult> resultPage = proxyRepository.findProxiesWithAssociations(protocolType, pageable);

        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<ProxyListQueryResult> content = resultPage.getContent();

        List<String> proxyIds = content.stream()
                .map(ProxyListQueryResult::getProxyDO)
                .map(ProxyDO::getId)
                .collect(Collectors.toList());
        Map<String, List<String>> domainsMap = proxyDomainRepository.findByProxyIdIn(proxyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ProxyDomainDO::getProxyId,
                        Collectors.mapping(ProxyDomainDO::getFullDomain, Collectors.toList())
                ));
        Map<String, List<ProxyTargetDO>> targetsMap = proxyTargetRepository.findByProxyIdIn(proxyIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(ProxyTargetDO::getProxyId));
        Map<String, List<TargetDTO>> targetDtoMap = new HashMap<>();
        List<HttpProxyListDTO> res = new ArrayList<>();
        for (ProxyListQueryResult r : content) {
            ProxyDO proxyDO = r.getProxyDO();
            AgentDO agentDO = r.getAgentDO();
            HttpProxyListDTO httpDTO = proxyConvert.toHttpListDTO(proxyDO, proxyPort);

            proxyConvert.enrichAgentType(httpDTO, agentDO);
            httpDTO.setDomains(domainsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            List<TargetDTO> targets = proxyTargetConvert.toDTOList(
                    targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            targetDtoMap.put(proxyDO.getId(), targets);
            httpDTO.setTargets(targets);
            httpDTO.setHttpProxyPort(proxyPort);
            res.add(httpDTO);
        }
        targetHealthEnricher.enrichBatch(targetDtoMap);
        return PageResult.wrap(resultPage, res);
    }

    @Override
    public HttpProxyDetailDTO getHttpProxyById(String id) {
        ProxyDO proxyDO = proxyRepository.findById(id)
                .filter(proxy -> proxy.getProtocol().isHttp())
                .orElseThrow(() -> new BizException("HTTP 代理不存在"));
        return buildHttpDetailDTO(proxyDO);
    }

    @Override
    public HttpsProxyDetailDTO getHttpsProxyById(String id) {
        ProxyDO proxyDO = proxyRepository.findById(id)
                .filter(proxy -> proxy.getProtocol().isHttps())
                .orElseThrow(() -> new BizException("HTTPS 代理不存在"));
        HttpsProxyDetailDTO dto = new HttpsProxyDetailDTO();
        fillHttpDetailDTO(dto, proxyDO);
        dto.setForceHttps(proxyDO.getForceHttps() == null || Boolean.TRUE.equals(proxyDO.getForceHttps()));
        return dto;
    }

    private HttpProxyDetailDTO buildHttpDetailDTO(ProxyDO proxyDO) {
        HttpProxyDetailDTO dto = new HttpProxyDetailDTO();
        fillHttpDetailDTO(dto, proxyDO);
        return dto;
    }

    private void fillHttpDetailDTO(HttpProxyDetailDTO dto, ProxyDO proxyDO) {
        List<ProxyTargetDO> targetRecords = proxyTargetRepository.findByProxyId(proxyDO.getId());
        List<ProxyDomainDO> domainRecords = proxyDomainRepository.findByProxyId(proxyDO.getId());

        dto.setId(proxyDO.getId());
        dto.setAgentId(proxyDO.getAgentId());
        dto.setName(proxyDO.getName());
        dto.setDomainType(domainTypeToCode(proxyDO.getDomainType()));
        fillHttpDomainFields(dto, proxyDO.getDomainType(), domainRecords);
        dto.setTargets(proxyTargetConvert.toDTOList(targetRecords));
        dto.setLoadBalance(buildLoadBalanceDTO(proxyDO));
        if (!targetRecords.isEmpty()) {
            ProxyTargetDO target = targetRecords.getFirst();
            dto.setLocalHost(target.getHost());
            dto.setLocalPort(target.getPort());
        }
        dto.setLimitTotal(toMbps(proxyDO.getLimitTotal()));
        dto.setCreatedAt(proxyDO.getCreatedAt());
        dto.setUpdatedAt(proxyDO.getUpdatedAt());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTcpProxy(TcpProxyCreateParam param) {
        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }

        String proxyId = uidGenerator.getUIDAsString();
        ProxyDO proxyDO = buildTcpProxyDO(param.getAgentId(), param.getName(), proxyId);
        applyTcpListenPort(proxyDO, param.getRemotePort(), null, null);
        applyTcpLimitTotal(proxyDO, param.getLimitTotal());
        proxyRepository.save(proxyDO);

        proxyTargetRepository.save(buildTcpTarget(param, proxyId));
        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, HealthCheckType.TCP));
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, true));
        logger.debug("TCP 代理创建成功：{}", proxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSocks5Proxy(Socks5ProxyCreateParam param) {
        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }
        String proxyId = uidGenerator.getUIDAsString();
        ProxyDO proxyDO = buildSocks5ProxyDO(param.getAgentId(), param.getName(), proxyId);
        applyTcpListenPort(proxyDO, param.getRemotePort(), null, null);
        applyTcpLimitTotal(proxyDO, param.getLimitTotal());
        proxyRepository.save(proxyDO);
        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        saveSocks5AuthConfig(proxyId, param.getAuthEnabled(), param.getAuthUsers(), true);
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, true));
        logger.debug("SOCKS5 代理创建成功：{}", proxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSocks5Proxy(Socks5ProxyUpdateParam param) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));
        if (!existsProxyDO.getProtocol().isSocks5()) {
            throw new BizException("仅支持 SOCKS5 代理");
        }
        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }
        Integer existsListenPort = existsProxyDO.getListenPort();
        Integer existsRemotePort = existsProxyDO.getRemotePort();
        existsProxyDO.setName(param.getName());
        applyTcpListenPort(existsProxyDO, param.getRemotePort(), existsListenPort, existsRemotePort);
        applyTcpLimitTotal(existsProxyDO, param.getLimitTotal());
        proxyRepository.save(existsProxyDO);
        if (param.getAuthEnabled() != null || !CollectionUtils.isEmpty(param.getAuthUsers())) {
            Boolean enabled = param.getAuthEnabled();
            if (enabled == null) {
                enabled = socks5AuthRepository.findById(proxyId)
                        .map(Socks5AuthDO::getEnabled)
                        .orElse(Boolean.FALSE);
            }
            saveSocks5AuthConfig(proxyId, enabled, param.getAuthUsers(), false);
        }
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, false));
        logger.debug("SOCKS5 代理更新成功：{}", existsProxyDO.getName());
    }

    @Override
    public Socks5ProxyDetailDTO getSocks5ProxyById(String id) {
        ProxyDO proxyDO = proxyRepository.findById(id)
                .filter(proxy -> proxy.getProtocol().isSocks5())
                .orElseThrow(() -> new BizException("SOCKS5 代理不存在"));
        Socks5ProxyDetailDTO dto = new Socks5ProxyDetailDTO();
        dto.setId(proxyDO.getId());
        dto.setAgentId(proxyDO.getAgentId());
        dto.setName(proxyDO.getName());
        dto.setRemotePort(proxyDO.getRemotePort());
        dto.setListenPort(proxyDO.getListenPort());
        dto.setLimitTotal(toMbps(proxyDO.getLimitTotal()));
        dto.setCreatedAt(proxyDO.getCreatedAt());
        dto.setUpdatedAt(proxyDO.getUpdatedAt());
        socks5AuthRepository.findById(id).ifPresent(authDO -> {
            dto.setAuthEnabled(Boolean.TRUE.equals(authDO.getEnabled()));
            List<Socks5UserDTO> users = socks5UserRepository.findByProxyId(id).stream()
                    .map(user -> {
                        Socks5UserDTO userDTO = new Socks5UserDTO();
                        userDTO.setId(user.getId());
                        userDTO.setUsername(user.getUsername());
                        return userDTO;
                    })
                    .toList();
            dto.setAuthUsers(users);
        });
        if (dto.getAuthEnabled() == null) {
            dto.setAuthEnabled(false);
        }
        return dto;
    }

    @Override
    public PageResult<Socks5ProxyListDTO> findSocks5Proxies(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());
        Page<ProxyDO> resultPage = proxyRepository.findByProtocolOrderByUpdatedAtDesc(ProtocolType.SOCKS5, pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<ProxyDO> content = resultPage.getContent();
        List<String> proxyIds = content.stream().map(ProxyDO::getId).toList();
        Map<String, Socks5AuthDO> authMap = socks5AuthRepository.findByProxyIdIn(proxyIds).stream()
                .collect(Collectors.toMap(Socks5AuthDO::getProxyId, a -> a));
        Map<String, Long> userCountMap = socks5UserRepository.findByProxyIdIn(proxyIds).stream()
                .collect(Collectors.groupingBy(Socks5UserDO::getProxyId, Collectors.counting()));
        List<Socks5ProxyListDTO> res = new ArrayList<>();
        for (ProxyDO proxyDO : content) {
            Socks5ProxyListDTO dto = proxyConvert.toSocks5ListDTO(proxyDO);
            Socks5AuthDO authDO = authMap.get(proxyDO.getId());
            dto.setAuthEnabled(authDO != null && Boolean.TRUE.equals(authDO.getEnabled()));
            dto.setAuthUserCount(userCountMap.getOrDefault(proxyDO.getId(), 0L).intValue());
            res.add(dto);
        }
        return PageResult.wrap(resultPage, res);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUdpProxy(UdpProxyCreateParam param) {
        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }

        String proxyId = uidGenerator.getUIDAsString();
        ProxyDO proxyDO = buildUdpProxyDO(param.getAgentId(), param.getName(), proxyId);
        applyUdpListenPort(proxyDO, param.getRemotePort(), null, null);
        applyTcpLimitTotal(proxyDO, param.getLimitTotal());
        proxyRepository.save(proxyDO);

        proxyTargetRepository.save(buildUdpTarget(param, proxyId));
        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, HealthCheckType.TCP));
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, true));
        logger.debug("UDP 代理创建成功：{}", proxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTcpProxy(TcpProxyUpdateParam param) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));
        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }

        Integer existsListenPort = existsProxyDO.getListenPort();
        Integer existsRemotePort = existsProxyDO.getRemotePort();
        existsProxyDO.setName(param.getName());
        applyTcpListenPort(existsProxyDO, param.getRemotePort(), existsListenPort, existsRemotePort);
        applyTcpLimitTotal(existsProxyDO, param.getLimitTotal());
        proxyRepository.save(existsProxyDO);

        replaceSingleTargetIfNotCluster(proxyId, () ->
                proxyTargetRepository.save(buildTcpTarget(param, proxyId)));
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, false));
        logger.debug("TCP 代理更新成功：{}", existsProxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUdpProxy(UdpProxyUpdateParam param) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));
        if (!existsProxyDO.getProtocol().isUdp()) {
            throw new BizException("仅支持 UDP 代理");
        }
        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }

        Integer existsListenPort = existsProxyDO.getListenPort();
        Integer existsRemotePort = existsProxyDO.getRemotePort();
        existsProxyDO.setName(param.getName());
        applyUdpListenPort(existsProxyDO, param.getRemotePort(), existsListenPort, existsRemotePort);
        applyTcpLimitTotal(existsProxyDO, param.getLimitTotal());
        proxyRepository.save(existsProxyDO);

        replaceSingleTargetIfNotCluster(proxyId, () ->
                proxyTargetRepository.save(buildUdpTarget(param, proxyId)));
        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, false));
        logger.debug("UDP 代理更新成功：{}", existsProxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveClusterConfig(String proxyId, ProxyClusterSaveParam param) {
        ProxyDO proxyDO = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("代理配置不存在"));

        LoadBalanceType strategy = LoadBalanceType.fromCode(param.getLoadBalance().getStrategy());
        if (strategy == null) {
            throw new BizException("无效的负载均衡策略");
        }

        List<ProxyTargetDO> targetRecords = proxyTargetRepository.findByProxyId(proxyId);
        List<ProxyTargetSaveParam> normalizedTargets = normalizeClusterTargets(param.getTargets());
        proxyDO.setLoadBalanceStrategy(strategy);
        proxyRepository.save(proxyDO);

        proxyTargetRepository.deleteByProxyId(proxyId);
        proxyTargetRepository.saveAll(proxyTargetConvert.toDOList(normalizedTargets, proxyId));
        cleanupStaleTargetHealth(proxyId, targetRecords, normalizedTargets);

        transactionHelper.afterCommit(() -> refreshRuntimeProxy(proxyId, false));
        logger.debug("代理 {} 负载均衡配置保存成功，后端数量: {}", proxyDO.getName(), normalizedTargets.size());
    }

    @Override
    public TcpProxyDetailDTO getTcpProxyById(String id) {
        ProxyDO proxyDO = proxyRepository.findById(id)
                .filter(proxy -> proxy.getProtocol().isTcp())
                .orElseThrow(() -> new BizException("TCP 代理不存在"));
        List<ProxyTargetDO> targetRecords = proxyTargetRepository.findByProxyId(id);

        TcpProxyDetailDTO dto = new TcpProxyDetailDTO();
        dto.setId(proxyDO.getId());
        dto.setAgentId(proxyDO.getAgentId());
        dto.setName(proxyDO.getName());
        dto.setRemotePort(proxyDO.getRemotePort());
        dto.setListenPort(proxyDO.getListenPort());
        dto.setTargets(proxyTargetConvert.toDTOList(targetRecords));
        dto.setLoadBalance(buildLoadBalanceDTO(proxyDO));
        if (!targetRecords.isEmpty()) {
            ProxyTargetDO target = targetRecords.getFirst();
            dto.setLocalHost(target.getHost());
            dto.setLocalPort(target.getPort());
        }
        dto.setLimitTotal(toMbps(proxyDO.getLimitTotal()));
        dto.setCreatedAt(proxyDO.getCreatedAt());
        dto.setUpdatedAt(proxyDO.getUpdatedAt());
        return dto;
    }

    @Override
    public UdpProxyDetailDTO getUdpProxyById(String id) {
        ProxyDO proxyDO = proxyRepository.findById(id)
                .filter(proxy -> proxy.getProtocol().isUdp())
                .orElseThrow(() -> new BizException("UDP 代理不存在"));
        List<ProxyTargetDO> targetRecords = proxyTargetRepository.findByProxyId(id);

        UdpProxyDetailDTO dto = new UdpProxyDetailDTO();
        dto.setId(proxyDO.getId());
        dto.setAgentId(proxyDO.getAgentId());
        dto.setName(proxyDO.getName());
        dto.setRemotePort(proxyDO.getRemotePort());
        dto.setListenPort(proxyDO.getListenPort());
        dto.setTargets(proxyTargetConvert.toDTOList(targetRecords));
        dto.setLoadBalance(buildLoadBalanceDTO(proxyDO));
        if (!targetRecords.isEmpty()) {
            ProxyTargetDO target = targetRecords.getFirst();
            dto.setLocalHost(target.getHost());
            dto.setLocalPort(target.getPort());
        }
        dto.setLimitTotal(toMbps(proxyDO.getLimitTotal()));
        dto.setCreatedAt(proxyDO.getCreatedAt());
        dto.setUpdatedAt(proxyDO.getUpdatedAt());
        return dto;
    }

    private ProxyDO buildUdpProxyDO(String agentId, String name, String proxyId) {
        ProxyDO proxyDO = new ProxyDO();
        proxyDO.setId(proxyId);
        proxyDO.setAgentId(agentId);
        proxyDO.setName(name);
        proxyDO.setProtocol(ProtocolType.UDP);
        proxyDO.setStatus(ProxyStatus.OPEN);
        proxyDO.setSourceType(ProxySourceType.MANUAL);
        proxyDO.setMultiplex(true);
        return proxyDO;
    }

    private void applyUdpListenPort(ProxyDO proxyDO, Integer requestRemotePort,
                                    Integer existsListenPort, Integer existsRemotePort) {
        if (requestRemotePort == null || requestRemotePort == 0) {
            if (existsListenPort != null) {
                proxyDO.setRemotePort(existsRemotePort);
                proxyDO.setListenPort(existsListenPort);
                return;
            }
            Integer listenPort = portPoolManager.acquire(PortPoolType.UDP);
            if (listenPort == null) {
                throw new BizException("没有可用 UDP 远程端口号");
            }
            proxyDO.setRemotePort(null);
            proxyDO.setListenPort(listenPort);
            transactionHelper.afterRollback(() -> portPoolManager.release(PortPoolType.UDP, listenPort));
            return;
        }

        if (Objects.equals(existsListenPort, requestRemotePort)) {
            proxyDO.setRemotePort(existsRemotePort);
            proxyDO.setListenPort(existsListenPort);
            return;
        }

        if (!portPoolManager.isAvailable(PortPoolType.UDP, requestRemotePort)) {
            throw new BizException("远程端口号不可用或被占用");
        }
        if (existsListenPort != null && !Objects.equals(existsListenPort, requestRemotePort)) {
            portPoolManager.release(PortPoolType.UDP, existsListenPort);
        }
        proxyDO.setRemotePort(requestRemotePort);
        proxyDO.setListenPort(requestRemotePort);
        portPoolManager.reserve(PortPoolType.UDP, requestRemotePort);
        transactionHelper.afterRollback(() -> portPoolManager.release(PortPoolType.UDP, requestRemotePort));
    }

    private ProxyTargetDO buildUdpTarget(UdpProxyCreateParam param, String proxyId) {
        ProxyTargetDO target = new ProxyTargetDO();
        target.setProxyId(proxyId);
        target.setHost(param.getLocalHost());
        target.setPort(param.getLocalPort());
        target.setName(param.getName());
        target.setWeight(1);
        return target;
    }

    private ProxyTargetDO buildUdpTarget(UdpProxyUpdateParam param, String proxyId) {
        ProxyTargetDO target = new ProxyTargetDO();
        target.setProxyId(proxyId);
        target.setHost(param.getLocalHost());
        target.setPort(param.getLocalPort());
        target.setName(param.getName());
        target.setWeight(1);
        return target;
    }

    private ProxyDO buildSocks5ProxyDO(String agentId, String name, String proxyId) {
        ProxyDO proxyDO = new ProxyDO();
        proxyDO.setId(proxyId);
        proxyDO.setAgentId(agentId);
        proxyDO.setName(name);
        proxyDO.setProtocol(ProtocolType.SOCKS5);
        proxyDO.setStatus(ProxyStatus.OPEN);
        proxyDO.setSourceType(ProxySourceType.MANUAL);
        return proxyDO;
    }

    private ProxyDO buildTcpProxyDO(String agentId, String name, String proxyId) {
        ProxyDO proxyDO = new ProxyDO();
        proxyDO.setId(proxyId);
        proxyDO.setAgentId(agentId);
        proxyDO.setName(name);
        proxyDO.setProtocol(ProtocolType.TCP);
        proxyDO.setStatus(ProxyStatus.OPEN);
        proxyDO.setSourceType(ProxySourceType.MANUAL);
        return proxyDO;
    }

    private void applyTcpListenPort(ProxyDO proxyDO, Integer requestRemotePort,
                                    Integer existsListenPort, Integer existsRemotePort) {
        if (requestRemotePort == null || requestRemotePort == 0) {
            if (existsListenPort != null) {
                proxyDO.setRemotePort(existsRemotePort);
                proxyDO.setListenPort(existsListenPort);
                return;
            }
            Integer listenPort = portPoolManager.acquire(PortPoolType.TCP);
            if (listenPort == null) {
                throw new BizException("没有可用远程端口号");
            }
            proxyDO.setRemotePort(null);
            proxyDO.setListenPort(listenPort);
            transactionHelper.afterRollback(() -> portPoolManager.release(PortPoolType.TCP, listenPort));
            return;
        }

        if (Objects.equals(existsListenPort, requestRemotePort)) {
            proxyDO.setRemotePort(existsRemotePort);
            proxyDO.setListenPort(existsListenPort);
            return;
        }

        if (!portPoolManager.isAvailable(PortPoolType.TCP, requestRemotePort)) {
            throw new BizException("远程端口号不可用或被占用");
        }
        if (existsListenPort != null && !Objects.equals(existsListenPort, requestRemotePort)) {
            portPoolManager.release(PortPoolType.TCP, existsListenPort);
        }
        proxyDO.setRemotePort(requestRemotePort);
        proxyDO.setListenPort(requestRemotePort);
        portPoolManager.reserve(PortPoolType.TCP, requestRemotePort);
        transactionHelper.afterRollback(() -> portPoolManager.release(PortPoolType.TCP, requestRemotePort));
    }

    private void applyTcpLimitTotal(ProxyDO proxyDO, Integer limitTotalMbps) {
        if (limitTotalMbps == null) {
            proxyDO.setLimitTotal(null);
            return;
        }
        proxyDO.setLimitTotal(BandwidthUnit.MBPS.toBps(limitTotalMbps));
    }

    private ProxyTargetDO buildTcpTarget(TcpProxyCreateParam param, String proxyId) {
        ProxyTargetDO target = new ProxyTargetDO();
        target.setProxyId(proxyId);
        target.setHost(param.getLocalHost());
        target.setPort(param.getLocalPort());
        target.setName(param.getName());
        target.setWeight(1);
        return target;
    }

    private ProxyTargetDO buildTcpTarget(TcpProxyUpdateParam param, String proxyId) {
        ProxyTargetDO target = new ProxyTargetDO();
        target.setProxyId(proxyId);
        target.setHost(param.getLocalHost());
        target.setPort(param.getLocalPort());
        target.setName(param.getName());
        target.setWeight(1);
        return target;
    }

    private Integer toMbps(Long bps) {
        if (bps == null) {
            return null;
        }
        return (int) (bps / BandwidthUnit.MBPS.getFactor());
    }

    private ProxyDO buildHttpLikeProxyDO(String agentId, String name, String proxyId, DomainType domainType,
                                         ProtocolType protocol, Boolean forceHttps) {
        ProxyDO proxyDO = new ProxyDO();
        proxyDO.setId(proxyId);
        proxyDO.setAgentId(agentId);
        proxyDO.setName(name);
        proxyDO.setProtocol(protocol);
        proxyDO.setStatus(ProxyStatus.OPEN);
        proxyDO.setSourceType(ProxySourceType.MANUAL);
        proxyDO.setDomainType(domainType);
        proxyDO.setMultiplex(true);
        proxyDO.setEncrypt(false);
        if (protocol.isHttps()) {
            proxyDO.setForceHttps(forceHttps == null || Boolean.TRUE.equals(forceHttps));
        }
        return proxyDO;
    }

    private void replaceSingleTargetIfNotCluster(String proxyId, Runnable saveSingleTarget) {
        List<ProxyTargetDO> existingTargets = proxyTargetRepository.findByProxyId(proxyId);
        if (existingTargets.size() > 1) {
            logger.debug("代理 {} 处于负载均衡模式，跳过内网目标更新", proxyId);
            return;
        }
        proxyTargetRepository.deleteByProxyId(proxyId);
        saveSingleTarget.run();
    }

    private LoadBalanceDTO buildLoadBalanceDTO(ProxyDO proxyDO) {
        LoadBalanceType strategy = proxyDO.getLoadBalanceStrategy();
        if (strategy == null) {
            strategy = LoadBalanceType.ROUND_ROBIN;
        }
        return new LoadBalanceDTO(strategy.getCode());
    }

    private List<ProxyTargetSaveParam> normalizeClusterTargets(List<ProxyTargetSaveParam> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            throw new BizException("请至少添加一个服务");
        }
        List<ProxyTargetSaveParam> normalized = new ArrayList<>(targets.size());
        for (ProxyTargetSaveParam target : targets) {
            if (target == null) {
                continue;
            }
            ProxyTargetSaveParam item = new ProxyTargetSaveParam();
            item.setHost(target.getHost().trim());
            item.setPort(target.getPort());
            item.setWeight(target.getWeight() == null || target.getWeight() < 1 ? 1 : target.getWeight());
            item.setName(StringUtils.hasText(target.getName()) ? target.getName().trim() : item.getHost());
            normalized.add(item);
        }
        if (normalized.isEmpty()) {
            throw new BizException("请至少添加一个服务");
        }
        return normalized;
    }

    private void cleanupStaleTargetHealth(String proxyId,
                                          List<ProxyTargetDO> previousTargets,
                                          List<ProxyTargetSaveParam> currentTargets) {
        if (CollectionUtils.isEmpty(previousTargets)) {
            return;
        }
        Set<String> currentKeys = currentTargets.stream()
                .map(target -> target.getHost() + ":" + target.getPort())
                .collect(Collectors.toSet());
        for (ProxyTargetDO previous : previousTargets) {
            String key = previous.getHost() + ":" + previous.getPort();
            if (!currentKeys.contains(key)) {
                healthManager.removeTarget(proxyId, previous.getHost(), previous.getPort());
            }
        }
    }

    private ProxyTargetDO buildHttpTarget(String localHost, Integer localPort, String name, String proxyId) {
        ProxyTargetDO target = new ProxyTargetDO();
        target.setProxyId(proxyId);
        target.setHost(localHost);
        target.setPort(localPort);
        target.setName(name);
        target.setWeight(1);
        return target;
    }

    private void applyHttpLimitTotal(ProxyDO proxyDO, Integer limitTotalMbps) {
        if (limitTotalMbps == null) {
            proxyDO.setLimitTotal(null);
            return;
        }
        proxyDO.setLimitTotal(BandwidthUnit.MBPS.toBps(limitTotalMbps));
    }

    private void assertHttpLikeProtocol(ProxyDO proxyDO, ProtocolType protocol) {
        if (protocol.isHttp() && !proxyDO.getProtocol().isHttp()) {
            throw new BizException("仅支持 HTTP 代理");
        }
        if (protocol.isHttps() && !proxyDO.getProtocol().isHttps()) {
            throw new BizException("仅支持 HTTPS 代理");
        }
    }

    private String resolveRootDomainForAuto() {
        List<String> rootDomains = appConfig.getRootDomains().stream().toList();
        if (!CollectionUtils.isEmpty(rootDomains)) {
            return rootDomains.getFirst();
        }
        return domainRepository.findAll().stream()
                .findFirst()
                .map(DomainDO::getDomain)
                .orElse(null);
    }

    private void validateHttpDomainInput(DomainType domainType, List<SubdomainBindingParam> subdomainBindings,
                                         List<String> customDomains) {
        if (domainType.isAuto()) {
            if (!StringUtils.hasText(resolveRootDomainForAuto())) {
                throw new BizException("未配置根域名，无法使用自动域名");
            }
            return;
        }
        if (domainType.isSubdomain()) {
            if (CollectionUtils.isEmpty(subdomainBindings)) {
                throw new BizException("请至少添加一条子域名配置");
            }
            Set<String> keys = new HashSet<>();
            for (SubdomainBindingParam binding : subdomainBindings) {
                if (binding == null) {
                    continue;
                }
                if (binding.getRootDomainId() == null) {
                    throw new BizException("请选择根域名");
                }
                if (!StringUtils.hasText(binding.getPrefix())) {
                    throw new BizException("请填写子域名前缀");
                }
                String prefix = binding.getPrefix().trim();
                String key = binding.getRootDomainId() + ":" + prefix;
                if (!keys.add(key)) {
                    throw new BizException("存在重复的子域名配置");
                }
                resolveSelectedRootDomain(binding.getRootDomainId());
            }
            return;
        }
        if (CollectionUtils.isEmpty(normalizeDomainValues(customDomains))) {
            throw new BizException("请填写自定义域名");
        }
    }

    private List<SubdomainBindingParam> normalizeSubdomainBindings(List<SubdomainBindingParam> bindings) {
        if (CollectionUtils.isEmpty(bindings)) {
            return List.of();
        }
        return bindings.stream()
                .filter(Objects::nonNull)
                .filter(binding -> StringUtils.hasText(binding.getPrefix()))
                .map(binding -> {
                    SubdomainBindingParam normalized = new SubdomainBindingParam();
                    normalized.setRootDomainId(binding.getRootDomainId());
                    normalized.setPrefix(binding.getPrefix().trim());
                    return normalized;
                })
                .toList();
    }

    private List<ResolvedSubdomainBinding> resolveSubdomainBindings(List<SubdomainBindingParam> bindings) {
        List<SubdomainBindingParam> normalized = normalizeSubdomainBindings(bindings);
        Set<String> keys = new HashSet<>();
        List<ResolvedSubdomainBinding> resolved = new ArrayList<>();
        for (SubdomainBindingParam binding : normalized) {
            String prefix = binding.getPrefix().trim();
            String key = binding.getRootDomainId() + ":" + prefix;
            if (!keys.add(key)) {
                throw new BizException("存在重复的子域名配置");
            }
            String rootDomain = resolveSelectedRootDomain(binding.getRootDomainId());
            resolved.add(new ResolvedSubdomainBinding(prefix, rootDomain));
        }
        return resolved;
    }

    private record ResolvedSubdomainBinding(String prefix, String rootDomain) {
        String fullDomain() {
            return prefix + "." + rootDomain;
        }
    }

    private List<String> normalizeDomainValues(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String resolveSelectedRootDomain(Integer rootDomainId) {
        if (rootDomainId == null) {
            throw new BizException("请选择根域名");
        }
        return domainRepository.findById(rootDomainId)
                .map(DomainDO::getDomain)
                .orElseThrow(() -> new BizException("根域名不存在"));
    }

    private void saveHttpDomains(String proxyId, DomainType domainType, List<SubdomainBindingParam> subdomainBindings,
                                 List<String> customDomains, String excludeProxyId) {
        if (domainType.isAuto()) {
            String rootDomain = resolveRootDomainForAuto();
            DomainInfo domain = domainGenerator.generateRandomSubdomain(rootDomain);
            proxyDomainRepository.save(new ProxyDomainDO(proxyId, domain.getDomain(), rootDomain, domainType));
            return;
        }
        if (domainType.isSubdomain()) {
            List<ResolvedSubdomainBinding> bindings = resolveSubdomainBindings(subdomainBindings);
            List<String> fullDomains = bindings.stream().map(ResolvedSubdomainBinding::fullDomain).toList();
            assertDomainsAvailable(fullDomains, excludeProxyId);
            proxyDomainRepository.saveAll(bindings.stream()
                    .map(binding -> new ProxyDomainDO(proxyId, binding.prefix(), binding.rootDomain(), domainType))
                    .toList());
            return;
        }
        List<String> domains = normalizeDomainValues(customDomains);
        assertDomainsAvailable(domains, excludeProxyId);
        proxyDomainRepository.saveAll(domains.stream()
                .map(domain -> new ProxyDomainDO(proxyId, domain, null, domainType))
                .toList());
    }

    private void assertDomainsAvailable(List<String> fullDomains, String excludeProxyId) {
        List<ProxyDomainDO> existsList = proxyDomainRepository.findByFullDomainIn(fullDomains);
        List<ProxyDomainDO> conflicts = existsList.stream()
                .filter(item -> excludeProxyId == null || !excludeProxyId.equals(item.getProxyId()))
                .toList();
        if (!conflicts.isEmpty()) {
            String existDomains = conflicts.stream()
                    .map(ProxyDomainDO::getFullDomain)
                    .collect(Collectors.joining(", "));
            throw new BizException("以下域名已被使用: " + existDomains);
        }
    }

    private void fillHttpDomainFields(HttpProxyDetailDTO dto, DomainType domainType, List<ProxyDomainDO> domainRecords) {
        if (domainType == null || CollectionUtils.isEmpty(domainRecords)) {
            return;
        }
        if (domainType.isSubdomain()) {
            Map<String, Integer> rootDomainIdMap = domainRepository.findAll().stream()
                    .collect(Collectors.toMap(DomainDO::getDomain, DomainDO::getId, (left, right) -> left));
            dto.setSubdomainBindings(domainRecords.stream().map(record -> {
                SubdomainBindingDTO binding = new SubdomainBindingDTO();
                binding.setPrefix(record.getDomain());
                binding.setRootDomain(record.getRootDomain());
                if (StringUtils.hasText(record.getRootDomain())) {
                    binding.setRootDomainId(rootDomainIdMap.get(record.getRootDomain()));
                }
                return binding;
            }).toList());
            return;
        }
        if (domainType.isCustomDomain()) {
            dto.setCustomDomains(domainRecords.stream().map(ProxyDomainDO::getDomain).toList());
        }
    }

    private Integer domainTypeToCode(DomainType domainType) {
        return domainType != null ? domainType.getCode() : null;
    }

    @Override
    public PageResult<TcpProxyListDTO> findTcpProxies(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());

        Page<ProxyDO> resultPage = proxyRepository.findByProtocolOrderByUpdatedAtDesc(ProtocolType.TCP, pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<ProxyDO> content = resultPage.getContent();
        List<String> proxyIds = content.stream().map(ProxyDO::getId).toList();
        Map<String, List<ProxyTargetDO>> targetsMap = proxyTargetRepository.findByProxyIdIn(proxyIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(ProxyTargetDO::getProxyId));
        Map<String, List<TargetDTO>> targetDtoMap = new HashMap<>();
        List<TcpProxyListDTO> res = new ArrayList<>();
        for (ProxyDO proxyDO : content) {
            TcpProxyListDTO tcpListDTO = proxyConvert.toTcpListDTO(proxyDO);
            List<TargetDTO> targets = proxyTargetConvert.toDTOList(
                    targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            targetDtoMap.put(proxyDO.getId(), targets);
            tcpListDTO.setTargets(targets);
            res.add(tcpListDTO);
        }
        targetHealthEnricher.enrichBatch(targetDtoMap);
        return PageResult.wrap(resultPage, res);
    }

    @Override
    public PageResult<UdpProxyListDTO> findUdpProxies(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());

        Page<ProxyDO> resultPage = proxyRepository.findByProtocolOrderByUpdatedAtDesc(ProtocolType.UDP, pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<ProxyDO> content = resultPage.getContent();
        List<String> proxyIds = content.stream().map(ProxyDO::getId).toList();
        Map<String, List<ProxyTargetDO>> targetsMap = proxyTargetRepository.findByProxyIdIn(proxyIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(ProxyTargetDO::getProxyId));
        List<UdpProxyListDTO> res = new ArrayList<>();
        for (ProxyDO proxyDO : content) {
            UdpProxyListDTO udpListDTO = proxyConvert.toUdpListDTO(proxyDO);
            List<TargetDTO> targets = proxyTargetConvert.toDTOList(
                    targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            udpListDTO.setTargets(targets);
            res.add(udpListDTO);
        }
        return PageResult.wrap(resultPage, res);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteProxies(ProxyBatchDeleteParam param) {
        List<String> ids = param.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ProtocolType protocolType = ProtocolType.fromCode(param.getProtocol());
        //Common
        proxyTargetRepository.deleteByProxyIdIn(ids);

        //IP CIDR
        accessControlRepository.deleteByProxyIdIn(ids);
        accessControlRuleRepository.deleteByProxyIdIn(ids);
        //HTTP / HTTPS
        if (protocolType.isHttpOrHttps()) {
            if (protocolType.isHttps()) {
                certBindingSyncService.removeBindingsByProxyIds(ids);
            }
            proxyDomainRepository.deleteByProxyIdIn(ids);
            basicAuthRepository.deleteByProxyIdIn(ids);
            basicUserRepository.deleteByProxyIdIn(ids);
        }
        if (protocolType.isSocks5()) {
            socks5AuthRepository.deleteByProxyIdIn(ids);
            socks5UserRepository.deleteByProxyIdIn(ids);
        }
        //删除流量统计数据
        ids.forEach(proxyId -> metricsService.deleteByProxyId(proxyId));
        //删除基础信息
        proxyRepository.deleteByIdIn(ids);
        //清空运行时数据
        transactionHelper.afterCommit(() -> proxyManager.deactivates(ids));
        logger.debug("批量删除代理成功，数量: {}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setProxyStatus(String id, Integer status) {
        ProxyDO proxyDO = proxyRepository.findById(id).orElseThrow(() -> new BizException("代理配置信息不存在"));
        ProxyStatus proxyStatus = ProxyStatus.fromCode(status);
        if (proxyStatus == null) {
            throw new BizException("无效的代理状态");
        }
        if (proxyDO.getStatus() == proxyStatus) {
            return;
        }
        if (proxyStatus.isOpen()) {
            validateProxyCanActivate(proxyDO);
        }
        proxyDO.setStatus(proxyStatus);
        proxyRepository.save(proxyDO);
        transactionHelper.afterCommit(() -> applyRuntimeStatus(proxyDO));
        logger.debug("代理状态更新成功：{} -> {}", proxyDO.getName(), proxyStatus.getDescription());
    }

    private void validateProxyCanActivate(ProxyDO proxyDO) {
        if (proxyDO.getProtocol().isHttpOrHttps()) {
            List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyId(proxyDO.getId());
            if (CollectionUtils.isEmpty(domains)) {
                throw new BizException("代理未配置可用域名，无法启用");
            }
            return;
        }
        if (proxyDO.getListenPort() == null) {
            throw new BizException("代理未配置远程端口，无法启用");
        }
    }

    private void applyRuntimeStatus(ProxyDO proxyDO) {
        refreshRuntimeProxy(proxyDO.getId(), false);
    }

    /**
     * 刷新服务端运行时注册（域名/端口监听）；非 SOCKS5 代理额外推送到在线客户端。
     * <p>
     * SOCKS5 的认证、监听与动态目标解析均在服务端完成，客户端仅按流打开消息连接目标，无需同步配置。
     */
    private void refreshRuntimeProxy(String proxyId, boolean newlyCreated) {
        ProxyDO proxyDO = proxyRepository.findById(proxyId).orElse(null);
        if (proxyDO == null) {
            return;
        }
        proxyConfigService.evictByProxyId(proxyId);
        if (proxyDO.getStatus().isClosed()) {
            proxyManager.deactivate(proxyId);
            return;
        }
        proxyManager.deactivate(proxyId);
        activateProxy(proxyDO);
        if (proxyDO.getProtocol().isSocks5()) {
            return;
        }
        if (newlyCreated) {
            proxyRuntimeSyncService.syncProxyCreated(proxyId);
        } else {
            proxyRuntimeSyncService.syncProxy(proxyId);
        }
    }

    private void activateProxy(ProxyDO proxyDO) {
        String agentId = proxyDO.getAgentId();
        String proxyId = proxyDO.getId();
        ProtocolType protocol = proxyDO.getProtocol();

        if (protocol.isHttpOrHttps()) {
            Set<String> domains = proxyDomainRepository.findByProxyId(proxyId).stream()
                    .map(ProxyDomainDO::getFullDomain)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            if (protocol.isHttp()) {
                proxyManager.registerHttp(agentId, proxyId, domains);
            } else {
                proxyManager.registerHttps(agentId, proxyId, domains);
            }
            return;
        }
        if (protocol.isUdp()) {
            proxyManager.registerUdp(agentId, proxyId, proxyDO.getListenPort());
            return;
        }
        if (protocol.isSocks5()) {
            proxyManager.registerSocks5(agentId, proxyId, proxyDO.getListenPort());
            return;
        }
        proxyManager.registerTcp(agentId, proxyId, proxyDO.getListenPort());
    }

    private void saveSocks5AuthConfig(String proxyId, Boolean authEnabled,
                                      List<Socks5AuthUserParam> authUsers, boolean creating) {
        boolean enabled = Boolean.TRUE.equals(authEnabled);
        if (creating && enabled) {
            validateSocks5AuthUsers(authUsers, true);
        }
        if (!creating && enabled && !CollectionUtils.isEmpty(authUsers)) {
            validateSocks5AuthUsers(authUsers, false);
        }

        Socks5AuthDO authDO = socks5AuthRepository.findById(proxyId).orElse(new Socks5AuthDO(proxyId, false));
        authDO.setEnabled(enabled);
        socks5AuthRepository.save(authDO);

        if (!enabled || CollectionUtils.isEmpty(authUsers)) {
            return;
        }

        List<Socks5UserDO> existingUsers = socks5UserRepository.findByProxyId(proxyId);
        Set<Long> incomingIds = authUsers.stream()
                .map(Socks5AuthUserParam::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        existingUsers.stream()
                .filter(user -> !incomingIds.contains(user.getId()))
                .forEach(user -> socks5UserRepository.deleteById(user.getId()));

        for (Socks5AuthUserParam userParam : authUsers) {
            if (!StringUtils.hasText(userParam.getUsername())) {
                throw new BizException("认证用户名不能为空");
            }
            if (userParam.getId() != null) {
                Socks5UserDO userDO = socks5UserRepository.findById(userParam.getId())
                        .orElseThrow(() -> new BizException("认证用户不存在"));
                if (!Objects.equals(userDO.getProxyId(), proxyId)) {
                    throw new BizException("认证用户不属于当前代理");
                }
                if (!Objects.equals(userParam.getUsername(), userDO.getUsername())
                        && socks5UserRepository.existsByProxyIdAndUsernameAndIdNot(
                        proxyId, userParam.getUsername(), userParam.getId())) {
                    throw new BizException("用户名已存在: " + userParam.getUsername());
                }
                userDO.setUsername(userParam.getUsername());
                if (StringUtils.hasText(userParam.getPassword())) {
                    userDO.setPassword(passwordEncoder.encode(userParam.getPassword()));
                } else if (creating) {
                    throw new BizException("认证密码不能为空");
                }
                socks5UserRepository.save(userDO);
                continue;
            }
            if (!StringUtils.hasText(userParam.getPassword())) {
                throw new BizException("认证密码不能为空");
            }
            if (socks5UserRepository.existsByProxyIdAndUsername(proxyId, userParam.getUsername())) {
                throw new BizException("用户名已存在: " + userParam.getUsername());
            }
            socks5UserRepository.save(new Socks5UserDO(
                    proxyId, userParam.getUsername(), passwordEncoder.encode(userParam.getPassword())));
        }
    }

    private void validateSocks5AuthUsers(List<Socks5AuthUserParam> authUsers, boolean requirePassword) {
        if (CollectionUtils.isEmpty(authUsers)) {
            throw new BizException("启用认证时必须至少添加一个用户");
        }
        for (Socks5AuthUserParam userParam : authUsers) {
            if (!StringUtils.hasText(userParam.getUsername())) {
                throw new BizException("认证用户名不能为空");
            }
            if (requirePassword && userParam.getId() == null && !StringUtils.hasText(userParam.getPassword())) {
                throw new BizException("认证密码不能为空");
            }
        }
    }
}
