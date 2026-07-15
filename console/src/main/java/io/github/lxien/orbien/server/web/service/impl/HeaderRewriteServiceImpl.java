/*
 *    Copyright 2026 lxien
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
package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.http.HeaderRewriteSupport;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.headerrewrite.HeaderRewriteDetailDTO;
import io.github.lxien.orbien.server.web.entity.HeaderRewriteDO;
import io.github.lxien.orbien.server.web.entity.HeaderRewriteRuleDO;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleAddParam;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleUpdateParam;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteUpdateParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.HeaderRewriteRepository;
import io.github.lxien.orbien.server.web.repository.HeaderRewriteRuleRepository;
import io.github.lxien.orbien.server.web.service.HeaderRewriteService;
import io.github.lxien.orbien.server.web.service.converter.HeaderRewriteConvert;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class HeaderRewriteServiceImpl implements HeaderRewriteService {
    @Autowired
    private HeaderRewriteRepository headerRewriteRepository;
    @Autowired
    private HeaderRewriteRuleRepository headerRewriteRuleRepository;
    @Autowired
    private HeaderRewriteConvert headerRewriteConvert;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HeaderRewriteDetailDTO getByProxyId(String proxyId) {
        HeaderRewriteDO rewriteDO = ensureParent(proxyId);
        List<HeaderRewriteRuleDO> rules = headerRewriteRuleRepository.findByProxyIdOrderByIdAsc(proxyId);
        return headerRewriteConvert.toDetailDTO(rewriteDO, rules);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HeaderRewriteUpdateParam param) {
        HeaderRewriteDO rewriteDO = ensureParent(param.getProxyId());
        rewriteDO.setEnabled(param.getEnabled());
        headerRewriteRepository.save(rewriteDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRule(HeaderRewriteRuleAddParam param) {
        ensureParent(param.getProxyId());
        if (headerRewriteRuleRepository.countByProxyId(param.getProxyId()) >= HeaderRewriteSupport.MAX_RULES) {
            throw new BizException("规则数量超过限制: " + HeaderRewriteSupport.MAX_RULES);
        }
        HeaderRewriteRuleDO ruleDO = headerRewriteConvert.toRuleDO(param);
        validateDomainRule(ruleDO);
        headerRewriteRuleRepository.save(ruleDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(HeaderRewriteRuleUpdateParam param) {
        HeaderRewriteRuleDO ruleDO = headerRewriteRuleRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("规则不存在"));
        if (!Objects.equals(ruleDO.getProxyId(), param.getProxyId())) {
            throw new BizException("代理 ID 不匹配");
        }
        headerRewriteConvert.updateRuleDO(ruleDO, param);
        validateDomainRule(ruleDO);
        headerRewriteRuleRepository.save(ruleDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        HeaderRewriteRuleDO ruleDO = headerRewriteRuleRepository.findById(id)
                .orElseThrow(() -> new BizException("规则不存在"));
        String proxyId = ruleDO.getProxyId();
        headerRewriteRuleRepository.deleteById(id);
        scheduleRefresh(proxyId);
    }

    private HeaderRewriteDO ensureParent(String proxyId) {
        return headerRewriteRepository.findById(proxyId)
                .orElseGet(() -> headerRewriteRepository.save(new HeaderRewriteDO(proxyId, false)));
    }

    private void validateDomainRule(HeaderRewriteRuleDO ruleDO) {
        try {
            HeaderRewriteRule rule = new HeaderRewriteRule(ruleDO.getAction(), ruleDO.getName(), ruleDO.getValue());
            HeaderRewriteSupport.validateRule(rule);
            ruleDO.setName(rule.getName());
            ruleDO.setValue(rule.getValue());
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage());
        }
    }

    private void scheduleRefresh(String proxyId) {
        transactionHelper.afterCommit(() -> proxyRuntimeSyncService.refreshServerEntryPolicy(proxyId));
    }
}
