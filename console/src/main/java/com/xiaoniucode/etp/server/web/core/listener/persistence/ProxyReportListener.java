/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.xiaoniucode.etp.server.web.core.listener.persistence;

import com.xiaoniucode.etp.core.domain.AccessControlConfig;
import com.xiaoniucode.etp.core.domain.BasicAuthConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.enums.*;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.notify.EventListener;
import com.xiaoniucode.etp.server.event.ProxyReportEvent;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import com.xiaoniucode.etp.server.web.core.converter.ProxyModelConvert;
import com.xiaoniucode.etp.server.web.entity.AccessControlDO;
import com.xiaoniucode.etp.server.web.entity.BasicAuthDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDomainDO;
import com.xiaoniucode.etp.server.web.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代理配置创建事件处理，用于持久化代理客户端注册的代理配置信息
 */
@Component
public class ProxyReportListener implements EventListener<ProxyReportEvent> {
    private final Logger logger = LoggerFactory.getLogger(ProxyReportListener.class);
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private TransportRepository transportRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private ProxyModelConvert proxyModelConvert;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(ProxyReportEvent event) {
        if (event.isUpdate() && !event.isHasChange()) {
            return;//更新操作，无数据变更
        }
        ProxyConfig config = event.getProxyConfig();
        transactionTemplate.executeWithoutResult(status -> {
            try {
                handleStandaloneAgent(event, config);
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("代理配置信息保存到数据库失败", e);
            }
        });
    }

    private void handleStandaloneAgent(ProxyReportEvent event, ProxyConfig config) {
        String proxyId = config.getProxyId();
        ProxyDO proxyDO = proxyModelConvert.toProxyDO(config);
        proxyDO.setSourceType(ProxySourceType.AGENT);
        if (config.getBandwidth() != null) {
            //带宽限流
            proxyModelConvert.updateProxyDO(proxyDO, config.getBandwidth());
        }
        AccessControlConfig accessControl = config.getAccessControl();
        if (accessControl != null) {
            //访问控制
            accessControlRepository.save(proxyModelConvert.toAccessControlDO(accessControl, proxyId));
            //访问控制规则
            accessControlRuleRepository.saveAll(proxyModelConvert.toAccessControlRuleDO(accessControl, proxyId));
        } else {
            accessControlRepository.save(new AccessControlDO(proxyId, AccessControl.DENY));
        }
        //删除所有目标服务
        proxyTargetRepository.deleteByProxyId(proxyId);
        //新的目标服务列表
        proxyTargetRepository.saveAll(proxyModelConvert.toProxyTargetDOList(config.getTargets(), proxyId));
        //自定义传输
        transportRepository.save(proxyModelConvert.toTransportDO(config.getTransport(), proxyId));

        if (config.isHttp()) {
            BasicAuthConfig basicAuth = config.getBasicAuth();
            if (basicAuth != null) {
                basicAuthRepository.save(new BasicAuthDO(proxyId, basicAuth.isEnabled()));
                basicUserRepository.saveAll(proxyModelConvert.toBasicUserDOList(basicAuth.getUsers(), proxyId));
            } else {
                basicAuthRepository.save(new BasicAuthDO(proxyId, false));
            }
            DomainType domainType = event.getDomainType();
            Set<DomainInfo> domains = event.getDomains();
            Set<ProxyDomainDO> proxyDomainDOS = domains.stream().map(domainInfo ->
                            new ProxyDomainDO(proxyId, domainInfo.getDomain(),
                                    domainInfo.getBaseDomain(), domainInfo.getDomainType()))
                    .collect(Collectors.toSet());

            proxyDomainRepository.deleteByProxyId(proxyId);
            proxyDomainRepository.saveAll(proxyDomainDOS);
            proxyDO.setDomainType(domainType);
        }
        //代理基础信息
        proxyRepository.save(proxyDO);
        logger.debug("代理配置信息已保存到数据库: agentId={} name={}", config.getAgentId(), config.getName());
    }
}
