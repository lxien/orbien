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

import io.github.lxien.orbien.server.generator.UUIDGenerator;
import io.github.lxien.orbien.server.service.TokenConfigService;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.accesstoken.AccessTokenDTO;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenBatchDeleteParam;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenCreateParam;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenUpdateParam;
import io.github.lxien.orbien.server.web.service.converter.AccessTokenConvert;
import io.github.lxien.orbien.server.web.entity.AccessTokenDO;
import io.github.lxien.orbien.server.web.repository.AccessTokenRepository;
import io.github.lxien.orbien.server.web.service.AccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private TokenConfigService tokenConfigService;

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
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
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
            tokenConfigService.evictByToken(accessTokenDO.getToken());
            accessTokenConvert.updateDO(accessTokenDO, param);
            accessTokenRepository.save(accessTokenDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Optional<AccessTokenDO> tokenOpt = accessTokenRepository.findById(id);
        if (tokenOpt.isPresent()) {
            AccessTokenDO accessTokenDO = tokenOpt.get();
            tokenConfigService.evictByToken(accessTokenDO.getToken());
            accessTokenRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(AccessTokenBatchDeleteParam param) {
        List<Integer> ids = param.getIds();
        if (ids != null && !ids.isEmpty()) {
            List<String> tokenList = accessTokenRepository.findAllById(ids)
                    .stream()
                    .map(AccessTokenDO::getToken)
                    .toList();
            tokenConfigService.evictByTokens(tokenList);
            accessTokenRepository.deleteAllById(ids);
        }
    }
}
