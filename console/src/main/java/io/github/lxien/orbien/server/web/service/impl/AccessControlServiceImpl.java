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

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.accesscontrol.AccessControlDetailDTO;
import io.github.lxien.orbien.server.web.entity.AccessControlDO;
import io.github.lxien.orbien.server.web.entity.AccessControlRuleDO;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlRuleAddParam;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlRuleUpdateParam;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlUpdateParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.AccessControlRepository;
import io.github.lxien.orbien.server.web.repository.AccessControlRuleRepository;
import io.github.lxien.orbien.server.web.service.AccessControlService;
import io.github.lxien.orbien.server.web.service.converter.AccessControlConvert;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class AccessControlServiceImpl implements AccessControlService {
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private AccessControlConvert accessControlConvert;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;

    @Override
    public AccessControlDetailDTO getByProxyId(String proxyId) {
        AccessControlDO accessControlDO = accessControlRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("访问控制配置不存在"));
        List<AccessControlRuleDO> accessControlRuleDOS = accessControlRuleRepository.findByProxyId(proxyId);
        return accessControlConvert.toDetailDTO(accessControlDO, accessControlRuleDOS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccessControl(AccessControlUpdateParam param) {
        String proxyId = param.getProxyId();
        AccessControlDO accessControlDO = accessControlRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("访问控制配置不存在"));
        accessControlConvert.updateDO(accessControlDO, param);
        accessControlRepository.save(accessControlDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRuleById(Long id) {
        AccessControlRuleDO accessControlRuleDO = accessControlRuleRepository.findById(id)
                .orElseThrow(() -> new BizException("规则不存在"));
        String proxyId = accessControlRuleDO.getProxyId();
        accessControlRuleRepository.deleteById(id);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRule(AccessControlRuleAddParam param) {
        String proxyId = param.getProxyId();
        accessControlRepository.findById(proxyId).orElseThrow(() -> new BizException("访问控制配置不存在"));
        if (accessControlRuleRepository.existsByProxyIdAndCidr(proxyId, param.getCidr())) {
            throw new BizException("已存在相同CIDR规则");
        }
        AccessControlRuleDO accessControlRuleDO = accessControlConvert.toRuleDO(param);
        accessControlRuleRepository.save(accessControlRuleDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(AccessControlRuleUpdateParam param) {
        AccessControlRuleDO ruleDO = accessControlRuleRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("规则不存在"));
        if (Objects.equals(ruleDO.getMode().getCode(), param.getRuleType())
                && Objects.equals(param.getCidr(), ruleDO.getCidr())) {
            return;
        }
        if (!Objects.equals(ruleDO.getCidr(), param.getCidr())
                && accessControlRuleRepository.existsByProxyIdAndCidrAndIdNot(
                        ruleDO.getProxyId(), param.getCidr(), param.getId())) {
            throw new BizException("该规则已存在");
        }
        String proxyId = ruleDO.getProxyId();
        accessControlConvert.updateRuleDO(ruleDO, param);
        accessControlRuleRepository.save(ruleDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    private void scheduleEntryPolicyRefresh(String proxyId) {
        transactionHelper.afterCommit(() -> proxyRuntimeSyncService.refreshServerEntryPolicy(proxyId));
    }
}
