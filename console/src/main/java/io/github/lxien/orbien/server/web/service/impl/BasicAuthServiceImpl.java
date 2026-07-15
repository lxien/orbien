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
import io.github.lxien.orbien.server.web.dto.basicauth.BasicAuthDetailDTO;
import io.github.lxien.orbien.server.web.entity.BasicAuthDO;
import io.github.lxien.orbien.server.web.entity.BasicUserDO;
import io.github.lxien.orbien.server.web.param.basicauth.BasicAuthUpdateParam;
import io.github.lxien.orbien.server.web.param.basicauth.httpuser.HttpUserAddParam;
import io.github.lxien.orbien.server.web.param.basicauth.httpuser.HttpUserUpdateParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.BasicAuthRepository;
import io.github.lxien.orbien.server.web.repository.BasicUserRepository;
import io.github.lxien.orbien.server.web.service.BasicAuthService;
import io.github.lxien.orbien.server.web.service.converter.BasicAuthConvert;
import io.github.lxien.orbien.server.transport.http.BasicAuthHandler;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class BasicAuthServiceImpl implements BasicAuthService {
    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private BasicAuthConvert basicAuthConvert;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;
    @Autowired
    private BasicAuthHandler basicAuthHandler;

    @Override
    public BasicAuthDetailDTO getByProxyId(String proxyId) {
        BasicAuthDO basicAuthDO = basicAuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("Basic Auth 配置不存在"));
        List<BasicUserDO> basicUserDOS = basicUserRepository.findByProxyId(proxyId);
        return basicAuthConvert.toDetailDTO(basicAuthDO, basicUserDOS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BasicAuthUpdateParam request) {
        String proxyId = request.getProxyId();
        BasicAuthDO basicAuthDO = basicAuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("Basic Auth 配置不存在"));
        basicAuthDO.setEnabled(request.getEnabled());
        basicAuthRepository.save(basicAuthDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(HttpUserAddParam request) {
        String proxyId = request.getProxyId();
        BasicAuthDO basicAuthDO = basicAuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("Basic Auth 配置不存在"));
        if (basicUserRepository.existsByProxyIdAndUsername(basicAuthDO.getProxyId(), request.getUsername())) {
            throw new BizException("用户名已存在");
        }
        BasicUserDO basicUserDO = basicAuthConvert.toUserDO(request);
        basicUserDO.setPassword(passwordEncoder.encode(request.getPassword()));
        basicUserRepository.save(basicUserDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(HttpUserUpdateParam param) {
        String proxyId = param.getProxyId();
        BasicUserDO basicUserDO = basicUserRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!Objects.equals(param.getUsername(), basicUserDO.getUsername())
                && basicUserRepository.existsByProxyIdAndUsernameAndIdNot(proxyId, param.getUsername(), param.getId())) {
            throw new BizException("用户名已存在");
        }
        basicAuthConvert.updateUserDO(basicUserDO, param);
        basicUserDO.setPassword(passwordEncoder.encode(param.getPassword()));
        basicUserRepository.save(basicUserDO);
        scheduleEntryPolicyRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        BasicUserDO basicUserDO = basicUserRepository.findById(id)
                .orElseThrow(() -> new BizException("用户不存在"));
        String proxyId = basicUserDO.getProxyId();
        basicUserRepository.deleteById(id);
        scheduleEntryPolicyRefresh(proxyId);
    }

    private void scheduleEntryPolicyRefresh(String proxyId) {
        transactionHelper.afterCommit(() -> {
            basicAuthHandler.invalidate(proxyId);
            proxyRuntimeSyncService.refreshServerEntryPolicy(proxyId);
        });
    }
}
