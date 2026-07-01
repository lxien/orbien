/*
 *    Copyright 2026 xiaoniucode
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
package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.core.enums.*;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.server.port.PortManager;
import com.xiaoniucode.etp.server.vhost.DomainGenerator;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.dto.proxy.*;
import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.param.bandwidth.BandwidthSaveParam;
import com.xiaoniucode.etp.server.web.param.proxy.*;
import com.xiaoniucode.etp.server.web.param.proxy.ProxyTargetSaveParam;
import com.xiaoniucode.etp.server.web.proxy.service.ProxyConfigSyncService;
import com.xiaoniucode.etp.server.web.repository.*;
import com.xiaoniucode.etp.server.web.service.MetricsService;
import com.xiaoniucode.etp.server.web.service.ProxyService;
import com.xiaoniucode.etp.server.web.service.assembler.ProxyAssembler;
import com.xiaoniucode.etp.server.web.service.converter.*;
import com.xiaoniucode.etp.server.web.support.tx.TransactionHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;

    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
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
    @Autowired
    private ProxyAssembler proxyAssembler;
    @Resource
    private AppConfig appConfig;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private DomainGenerator domainGenerator;
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private PortManager portManager;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ProxyConfigSyncService proxyConfigSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHttpProxy(HttpProxyCreateParam param) {

        DomainType domainType = DomainType.fromCode(param.getDomainType());
        String rootDomain=appConfig.getRootDomains().stream().toList().getFirst();
        if (StringUtils.hasText(rootDomain) && (domainType.isAuto() || domainType.isSubdomain())) {
            throw new BizException("不支持改域名类型的自动生成！");
        }
        //1.基础信息
        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("名称已经存在: " + param.getName());
        }
        String proxyId = uidGenerator.getUIDAsString();
        ProxyDO proxyDO = proxyConvert.toDO(param, proxyId);
        BandwidthSaveParam bandwidth = param.getBandwidth();
        if (bandwidth != null) {
            bandwidth.valid();
            BandwidthUnit unit = BandwidthUnit.fromCode(bandwidth.getUnit());
            if (bandwidth.getLimitTotal() != null) {
                proxyDO.setLimitTotal(unit.toBps(bandwidth.getLimitTotal()));
            }
            if (bandwidth.getLimitIn() != null) {
                proxyDO.setLimitIn(unit.toBps(bandwidth.getLimitIn()));
            }
            if (bandwidth.getLimitOut() != null) {
                proxyDO.setLimitOut(unit.toBps(bandwidth.getLimitOut()));
            }
        }
        proxyRepository.save(proxyDO);
        //2.服务
        if (proxyDO.getDeploymentMode().isStandalone() && param.getTargets().size() > 1) {
            throw new BizException("单机模式只能配置一个服务节点");
        }
        proxyTargetRepository.saveAll(proxyTargetConvert.toDOList(param.getTargets(), proxyId));

        //4.传输

        //5.域名
        Set<String> fullDomains = new HashSet<>();
        if (domainType.isAuto()) {
            DomainInfo domain = domainGenerator.generateRandomSubdomain(rootDomain);
            fullDomains.add(domain.getFullDomain());
            proxyDomainRepository.save(new ProxyDomainDO(proxyId, domain.getDomain(), rootDomain, domainType));
        } else if (domainType.isCustomDomain()) {
            Set<String> domains = param.getDomains();
            List<ProxyDomainDO> existsList = proxyDomainRepository.findByFullDomainIn(domains);
            if (!existsList.isEmpty()) {
                String existDomains = existsList.stream()
                        .map(ProxyDomainDO::getDomain)
                        .collect(Collectors.joining(", "));
                throw new BizException("以下域名已被使用: " + existDomains);
            }
            List<ProxyDomainDO> list = domains.stream()
                    .map(domain -> new ProxyDomainDO(proxyId, domain, null, domainType)).toList();
            proxyDomainRepository.saveAll(list);
            fullDomains.addAll(domains);
        } else if (domainType.isSubdomain()) {
            Set<String> prefixes = param.getDomains();
            List<String> domains = prefixes.stream().map(prefix -> prefix + "." + rootDomain).toList();
            List<ProxyDomainDO> existsList = proxyDomainRepository.findByFullDomainIn(domains);
            if (!existsList.isEmpty()) {
                String existDomains = existsList.stream()
                        .map(ProxyDomainDO::getDomain)
                        .collect(Collectors.joining(", "));
                throw new BizException("以下子域名已被使用: " + existDomains);
            }
            fullDomains.addAll(domains);
            List<ProxyDomainDO> list = prefixes.stream()
                    .map(prefix -> new ProxyDomainDO(proxyId, prefix, rootDomain, domainType)).toList();
            proxyDomainRepository.saveAll(list);
        }

        //6.初始化访问控制
        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        //7.初始化BasicAuth认证
        basicAuthRepository.save(new BasicAuthDO(proxyId, false));
        //8.初始化健康检查
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, HealthCheckType.HTTP));


       // proxyConfigSyncService.syncOnCreate(ext);

        if (proxyDO.getStatus().isOpen()) {
            ProxyConfig proxyConfig = proxyAssembler.toProxyConfig(proxyDO);
            ProxyConfigExt ext = ProxyConfigExt.of(proxyConfig, null);

//            transactionHelper.afterCommit(() ->
//                    proxyManager.activate(proxyConfig));
        }


        logger.debug("HTTP代理创建成功：{}", proxyDO.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHttpProxy(HttpProxyUpdateParam param) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId).orElseThrow(() -> new BizException("代理配置不存在"));
        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("代理名称已经存在: " + param.getName());
        }
        DomainType existsDomainType = existsProxyDO.getDomainType();
        DomainType requestDomainType = DomainType.fromCode(param.getDomainType());
        BandwidthSaveParam bandwidth = param.getBandwidth();
        if (bandwidth != null) {
            bandwidth.valid();
            BandwidthUnit unit = BandwidthUnit.fromCode(bandwidth.getUnit());
            existsProxyDO.setLimitTotal(bandwidth.getLimitTotal() != null ? unit.toBps(bandwidth.getLimitTotal()) : null);
            existsProxyDO.setLimitIn(bandwidth.getLimitIn() != null ? unit.toBps(bandwidth.getLimitIn()) : null);
            existsProxyDO.setLimitOut(bandwidth.getLimitOut() != null ? unit.toBps(bandwidth.getLimitOut()) : null);
        }

        proxyConvert.updateDO(param, existsProxyDO);
        proxyRepository.save(existsProxyDO);

        //3.服务列表
        if (existsProxyDO.getDeploymentMode().isStandalone() && param.getTargets().size() > 1) {
            throw new BizException("单机服务只能配置一个目标节点");
        }
        proxyTargetRepository.deleteByProxyId(proxyId);
        if (!CollectionUtils.isEmpty(param.getTargets())) {
            proxyTargetRepository.saveAll(proxyTargetConvert.toDOList(param.getTargets(), proxyId));
        }
        //6.传输

        //7.HTTP域名信息
        if (!(existsDomainType == requestDomainType && existsDomainType.isAuto())) {
            proxyDomainRepository.deleteByProxyId(proxyId);
        }
        //todo String rootDomain = appConfig.getBaseDomain();
        String rootDomain=appConfig.getRootDomains().stream().toList().getFirst();
        Set<String> fullDomains = new HashSet<>();
        //请求域名类型和存在域名相同且是自动生成域名类型的时保持不变，其他都删除后重新生成
        if (!((requestDomainType == existsDomainType) && existsDomainType.isAuto())) {
            proxyDomainRepository.deleteByProxyId(proxyId);
        } else {
            //自动类型域名且未变化
            List<ProxyDomainDO> proxyDomainDOS = proxyDomainRepository.findByProxyId(proxyId);
            if (!CollectionUtils.isEmpty(proxyDomainDOS)) {
                proxyDomainDOS.forEach(domainDO -> fullDomains.add(domainDO.getFullDomain()));
            }
        }
        if (requestDomainType.isAuto() && !existsDomainType.isAuto()) {
            DomainInfo domainInfo = domainGenerator.generateRandomSubdomain(rootDomain);
            fullDomains.add(domainInfo.getFullDomain());
            proxyDomainRepository.save(new ProxyDomainDO(proxyId, domainInfo.getDomain(), rootDomain, requestDomainType));
        } else if (requestDomainType.isCustomDomain()) {
            Set<String> domains = param.getDomains();
            List<ProxyDomainDO> existsList = proxyDomainRepository.findByFullDomainIn(domains);
            if (!existsList.isEmpty()) {
                String existDomains = existsList.stream().map(ProxyDomainDO::getDomain).collect(Collectors.joining(", "));
                throw new BizException("域名已被占用: " + existDomains);
            }
            fullDomains.addAll(domains);
            List<ProxyDomainDO> list = domains.stream()
                    .map(domain -> new ProxyDomainDO(proxyId, domain, null, requestDomainType)).toList();
            proxyDomainRepository.saveAll(list);
        } else if (requestDomainType.isSubdomain()) {
            Set<String> prefixes = param.getDomains();
            List<String> domains = prefixes.stream().map(prefix -> prefix + "." + rootDomain).toList();
            List<ProxyDomainDO> existsList = proxyDomainRepository.findByFullDomainIn(domains);
            if (!existsList.isEmpty()) {
                String existDomains = existsList.stream()
                        .map(ProxyDomainDO::getDomain)
                        .collect(Collectors.joining(", "));
                throw new BizException("域名已被占用: " + existDomains);
            }
            fullDomains.addAll(domains);
            List<ProxyDomainDO> list = prefixes.stream()
                    .map(prefix -> new ProxyDomainDO(proxyId, prefix, rootDomain, requestDomainType)).toList();
            proxyDomainRepository.saveAll(list);
        }
//        transactionHelper.afterCommit(() ->
//                proxyManager.reconcile(proxyAssembler.toProxyConfig(existsProxyDO), fullDomains));
        logger.debug("HTTP代理更新成功：{}", existsProxyDO.getName());
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
    public PageResult<HttpProxyListDTO> findHttpsProxies(PageQuery pageQuery) {
        return findHttpLikeProxies(pageQuery, ProtocolType.HTTPS, appConfig.getHttpsProxyPort());
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
        List<HttpProxyListDTO> res = new ArrayList<>();
        for (ProxyListQueryResult r : content) {
            ProxyDO proxyDO = r.getProxyDO();
            AgentDO agentDO = r.getAgentDO();
            HttpProxyListDTO httpDTO = proxyConvert.toHttpListDTO(proxyDO, proxyPort);

            if (agentDO != null && agentDO.getAgentType() != null) {
                httpDTO.setAgentType(agentDO.getAgentType().getCode());
            }
            httpDTO.setDomains(domainsMap.getOrDefault(proxyDO.getId(), Collections.emptyList()));
            httpDTO.setTargets(proxyTargetConvert.toDTOList(targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList())));
            httpDTO.setHttpProxyPort(proxyPort);
            res.add(httpDTO);
        }
        return PageResult.wrap(resultPage, res);
    }

    @Override
    public HttpProxyDetailDTO getHttpProxyById(String id) {
        ProxyDetailQueryResult detail = proxyRepository.findDetailByProxyId(id);
        AgentDO agentDO = detail.getAgentDO();
        ProxyDO proxyDO = detail.getProxyDO();

        List<ProxyTargetDO> proxyTargetDos = proxyTargetRepository.findByProxyId(id);
        List<ProxyDomainDO> httpProxyDomainDOs = proxyDomainRepository.findByProxyId(id);

        HttpProxyDetailDTO httpProxyDetailDTO = proxyConvert.toHttpDetailDTO(proxyDO, agentDO.getAgentType().getCode());

        List<TargetDTO> targetDTOList = proxyTargetConvert.toDTOList(proxyTargetDos);

        httpProxyDetailDTO.setTargets(targetDTOList);
        List<String> domains = httpProxyDomainDOs.stream().map(ProxyDomainDO::getDomain).toList();
        httpProxyDetailDTO.setDomains(domains);
        return httpProxyDetailDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTcpProxy(TcpProxyCreateParam param) {
        String proxyId = uidGenerator.getUIDAsString();
        //1.基础信息
        if (proxyRepository.existsByAgentIdAndName(param.getAgentId(), param.getName())) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }
        ProxyDO proxyDO = proxyConvert.toDO(param, proxyId);
        Integer remotePort = param.getRemotePort();
        if (remotePort == null || remotePort == 0) {
            Integer acquire = portManager.acquire();
            if (acquire == null) {
                throw new BizException("没有可用远程端口号");
            }
            proxyDO.setRemotePort(acquire);
            proxyDO.setListenPort(acquire);
        } else if (!portManager.isAvailable(remotePort)) {
            throw new BizException("远程端口号不可用或被占用");
        } else {
            proxyDO.setListenPort(remotePort);
            portManager.addPort(remotePort);
            transactionHelper.afterRollback(() -> portManager.release(remotePort));
        }
        //2.带宽
        BandwidthSaveParam bandwidth = param.getBandwidth();
        if (bandwidth != null) {
            bandwidth.valid();
            BandwidthUnit unit = BandwidthUnit.fromCode(bandwidth.getUnit());
            if (bandwidth.getLimitTotal() != null) {
                proxyDO.setLimitTotal(unit.toBps(bandwidth.getLimitTotal()));
            }
            if (bandwidth.getLimitIn() != null) {
                proxyDO.setLimitIn(unit.toBps(bandwidth.getLimitIn()));
            }
            if (bandwidth.getLimitOut() != null) {
                proxyDO.setLimitOut(unit.toBps(bandwidth.getLimitOut()));
            }
        }
        proxyRepository.save(proxyDO);
        //3.服务列表
        if (proxyDO.getDeploymentMode().isStandalone() && param.getTargets().size() > 1) {
            throw new BizException("单机服务只能配置一个目标节点");
        }
        List<ProxyTargetDO> proxyTargetDOList = proxyTargetConvert.toDOList(param.getTargets(), proxyId);
        proxyTargetRepository.saveAll(proxyTargetDOList);

        //5.传输

        //6.初始化访问控制
        accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        //7.初始化健康检查
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, HealthCheckType.TCP));

//        if (proxyDO.getStatus().isOpen()) {
//            transactionHelper.afterCommit(() ->
//                    proxyManager.activate(proxyAssembler.toProxyConfig(proxyDO)));
//        }
        logger.debug("TCP 代理创建成功：{}", proxyDO.getName());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTcpProxy(TcpProxyUpdateParam param) {
        String proxyId = param.getId();
        ProxyDO existsProxyDO = proxyRepository.findById(proxyId).orElseThrow(() -> new BizException("代理配置不存在"));
        if (proxyRepository.existsByAgentIdAndNameAndIdNot(
                existsProxyDO.getAgentId(), param.getName(), proxyId)) {
            throw new BizException("该客户端下已存在同名代理名称: " + param.getName());
        }
        //1.基本信息
        DeploymentMode existsDeploymentMode = existsProxyDO.getDeploymentMode();
        DeploymentMode requestDeploymentMode = DeploymentMode.fromCode(param.getDeploymentMode());
        Integer existsListenPort = existsProxyDO.getListenPort();
        Integer requestRemotePort = param.getRemotePort();
        proxyConvert.updateDO(param, existsProxyDO);
        if (requestRemotePort == null) {
            existsProxyDO.setRemotePort(existsProxyDO.getListenPort());
        } else if (!Objects.equals(existsListenPort, requestRemotePort)) {
            if (!portManager.isAvailable(requestRemotePort)) {
                throw new BizException("远程端口号不可用或被占用");
            }
            existsProxyDO.setRemotePort(requestRemotePort);
            existsProxyDO.setListenPort(requestRemotePort);
            portManager.addPort(requestRemotePort);
            transactionHelper.afterRollback(() -> portManager.release(requestRemotePort));
        }
        BandwidthSaveParam bandwidth = param.getBandwidth();
        if (bandwidth != null) {
            bandwidth.valid();
            BandwidthUnit unit = BandwidthUnit.fromCode(bandwidth.getUnit());
            existsProxyDO.setLimitTotal(bandwidth.getLimitTotal() != null ? unit.toBps(bandwidth.getLimitTotal()) : null);
            existsProxyDO.setLimitIn(bandwidth.getLimitIn() != null ? unit.toBps(bandwidth.getLimitIn()) : null);
            existsProxyDO.setLimitOut(bandwidth.getLimitOut() != null ? unit.toBps(bandwidth.getLimitOut()) : null);
        }
        proxyRepository.save(existsProxyDO);

        //3.服务
        List<ProxyTargetSaveParam> targets = param.getTargets();
        if (existsProxyDO.getDeploymentMode().isStandalone() && targets.size() > 1) {
            throw new BizException("单机模式只能配置一个目标节点");
        }
        proxyTargetRepository.deleteByProxyId(proxyId);
        proxyTargetRepository.saveAll(proxyTargetConvert.toDOList(targets, proxyId));

        //5.传输
        transactionHelper.afterCommit(() -> {
           // proxyManager.reconcile(proxyAssembler.toProxyConfig(existsProxyDO));
        });
        logger.debug("TCP 代理更新成功：{}", existsProxyDO.getName());
    }

    @Override
    public TcpProxyDetailDTO getTcpProxyById(String id) {
        ProxyDetailQueryResult detail = proxyRepository.findDetailByProxyId(id);
        ProxyDO proxyDO = detail.getProxyDO();


        List<ProxyTargetDO> proxyTargetDos = proxyTargetRepository.findByProxyId(id);
        TcpProxyDetailDTO tcpProxyDetailDTO = proxyConvert.toTcpDetailDTO(proxyDO, detail.getAgentDO().getAgentType().getCode());

        tcpProxyDetailDTO.setTargets(proxyTargetConvert.toDTOList(proxyTargetDos));
        return tcpProxyDetailDTO;
    }

    @Override
    public PageResult<TcpProxyListDTO> findTcpProxies(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());

        Page<ProxyListQueryResult> resultPage = proxyRepository.findProxiesWithAssociations(ProtocolType.TCP, pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<ProxyListQueryResult> content = resultPage.getContent();
        List<String> proxyIds = content.stream().map(ProxyListQueryResult::getProxyDO).map(ProxyDO::getId).toList();
        Map<String, List<ProxyTargetDO>> targetsMap = proxyTargetRepository.findByProxyIdIn(proxyIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(ProxyTargetDO::getProxyId));
        List<TcpProxyListDTO> res = new ArrayList<>();
        for (ProxyListQueryResult r : content) {
            ProxyDO proxyDO = r.getProxyDO();
            AgentDO agentDO = r.getAgentDO();
            TcpProxyListDTO tcpListDTO = proxyConvert.toTcpListDTO(proxyDO);
            tcpListDTO.setAgentType(agentDO.getAgentType().getCode());
            tcpListDTO.setTargets(proxyTargetConvert.toDTOList(targetsMap.getOrDefault(proxyDO.getId(), Collections.emptyList())));
            res.add(tcpListDTO);
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
        //HTTP
        if (protocolType.isHttp()) {
            proxyDomainRepository.deleteByProxyIdIn(ids);
            basicAuthRepository.deleteByProxyIdIn(ids);
            basicUserRepository.deleteByProxyIdIn(ids);
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
        if (proxyDO.getStatus().getCode().equals(status)) {
            return;
        }
        proxyDO.setStatus(ProxyStatus.fromCode(status));
        proxyRepository.save(proxyDO);
    }
}
