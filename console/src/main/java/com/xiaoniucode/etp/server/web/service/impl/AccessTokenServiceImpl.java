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

import com.xiaoniucode.etp.server.generator.UUIDGenerator;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.accesstoken.AccessTokenDTO;
import com.xiaoniucode.etp.server.web.param.accesstoken.AccessTokenBatchDeleteParam;
import com.xiaoniucode.etp.server.web.param.accesstoken.AccessTokenCreateParam;
import com.xiaoniucode.etp.server.web.param.accesstoken.AccessTokenUpdateParam;
import com.xiaoniucode.etp.server.web.service.converter.AccessTokenConvert;
import com.xiaoniucode.etp.server.web.entity.AccessTokenDO;
import com.xiaoniucode.etp.server.web.repository.AccessTokenRepository;
import com.xiaoniucode.etp.server.web.service.AccessTokenService;
import com.xiaoniucode.etp.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private UUIDGenerator uuidGenerator;
    @Autowired
    private AccessTokenConvert accessTokenConvert;
    @Autowired
    private TransactionHelper transactionHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessTokenDTO create(AccessTokenCreateParam param) {
        if (accessTokenRepository.existsByName(param.getName())) {
            throw new BizException("令牌名称已存在");
        }

        AccessTokenDO accessToken = accessTokenConvert.toDO(param);
        String token = uuidGenerator.uuid32().toUpperCase();
        accessToken.setToken(token);
        AccessTokenDO save = accessTokenRepository.save(accessToken);

        return accessTokenConvert.toDTOWithFullToken(save);
    }

    @Override
    public PageResult<AccessTokenDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());
        Page<AccessTokenDO> tokenPage = accessTokenRepository.findAll(pageable);

        if (tokenPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }

        List<AccessTokenDO> tokens = tokenPage.getContent();
        List<AccessTokenDTO> tokenDTOList = accessTokenConvert.toDTOList(tokens);
        return PageResult.wrap(tokenPage, tokenDTOList);
    }

    @Override
    public AccessTokenDTO findById(Integer id) {
        AccessTokenDO accessTokenDO = accessTokenRepository.findById(id).orElseThrow(() ->
                new BizException("令牌不存在"));
        return accessTokenConvert.toDTO(accessTokenDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AccessTokenUpdateParam param) {
        AccessTokenDO accessTokenDO = accessTokenRepository.findById(param.getId()).orElse(null);
        if (accessTokenDO != null) {
            if (accessTokenRepository.existsByNameAndIdNot(param.getName(), param.getId())) {
                throw new BizException("令牌名称已存在");
            }
            accessTokenConvert.updateDO(accessTokenDO, param);
            accessTokenRepository.save(accessTokenDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Optional<AccessTokenDO> tokenOpt = accessTokenRepository.findById(id);
        if (tokenOpt.isPresent()) {
            accessTokenRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(AccessTokenBatchDeleteParam param) {
        List<Integer> ids = param.getIds();
        if (ids != null && !ids.isEmpty()) {
            accessTokenRepository.deleteAllById(ids);
        }
    }
}
