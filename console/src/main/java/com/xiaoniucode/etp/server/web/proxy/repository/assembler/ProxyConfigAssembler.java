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

package com.xiaoniucode.etp.server.web.proxy.repository.assembler;

import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.enums.AccessControl;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.server.web.proxy.converter.ProxyModelConvert;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult;
import com.xiaoniucode.etp.server.web.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class ProxyConfigAssembler {
    @Autowired
    private ProxyModelConvert proxyModelConvert;

    public ProxyConfig assembleBase(ProxyDetailQueryResult result) {
        if (result == null) {
            return null;
        }
        AgentDO agentDO = result.getAgentDO();
        ProxyDO proxyDO = result.getProxyDO();

        ProxyConfig config = proxyModelConvert.toProxyConfig(proxyDO);
        config.setAgentId(agentDO.getId());
        config.setAgentType(agentDO.getAgentType());
        config.setListenPort(config.getRemotePort());


        if (proxyDO.getLimitTotal() != null || proxyDO.getLimitIn() != null || proxyDO.getLimitOut() != null) {
            config.setBandwidth(new BandwidthConfig(proxyDO.getLimitTotal(), proxyDO.getLimitIn(), proxyDO.getLimitOut()));
        }

      //todo transport
        AccessControlDO accessControlDO = result.getAccessControlDO();
        if (accessControlDO != null) {
            config.setAccessControl(proxyModelConvert.toAccessControlConfig(accessControlDO));
        }
        BasicAuthDO basicAuthDO = result.getBasicAuthDO();
        if (config.isHttp() && basicAuthDO != null) {
            config.setBasicAuth(proxyModelConvert.toBasicAuthConfig(basicAuthDO));
        }
        return config;
    }

    public void assembleTargets(ProxyConfig config, List<ProxyTargetDO> targets) {
        List<Target> targetModels = proxyModelConvert.toTargetModel(targets);
        config.addTargets(targetModels);
    }

    public void assembleDomains(ProxyConfig config, List<ProxyDomainDO> domainDOs) {
        if (CollectionUtils.isEmpty(domainDOs)) {
            return;
        }
        RouteConfig routeConfig = new RouteConfig();
        for (ProxyDomainDO domainDO : domainDOs) {
            DomainType domainType = domainDO.getDomainType();
            if (domainType.isAuto()) {
                routeConfig.setAutoDomain(true);
            } else if (domainType.isCustomDomain()) {
                routeConfig.addCustomDomain(domainDO.getDomain());
            } else if (domainType.isSubdomain()) {
                routeConfig.addSubDomain(domainDO.getDomain());
            }
        }
        config.setRouteConfig(routeConfig);
    }

    public List<ProxyConfig> assembleList(List<ProxyDO> list) {
        return proxyModelConvert.toProxyConfig(list);
    }

    public void assembleBasicAuthUsers(ProxyConfig config, List<BasicUserDO> basicUsers) {
        BasicAuthConfig basicAuth = config.getBasicAuth();
        if (basicAuth != null) {
            basicAuth.addUsers(proxyModelConvert.toBasicAuthUserConfig(basicUsers));
        }
    }

    public void assembleAccessControlRules(ProxyConfig config, List<AccessControlRuleDO> accessControlRuleDOS) {
        if (CollectionUtils.isEmpty(accessControlRuleDOS)) {
            return;
        }
        AccessControlConfig accessControl = config.getAccessControl();
        accessControlRuleDOS.forEach(rule -> {
            AccessControl mode = rule.getMode();
            if (mode.isAllowMode()) {
                accessControl.addAllow(rule.getCidr());
            } else if (mode.isDenyMode()) {
                accessControl.addDeny(rule.getCidr());
            }
        });
    }
}
