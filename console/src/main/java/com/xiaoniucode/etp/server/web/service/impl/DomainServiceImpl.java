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

package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.server.service.DomainConfigService;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.domain.DomainDTO;
import com.xiaoniucode.etp.server.web.dto.domain.UsedDomainDTO;
import com.xiaoniucode.etp.server.web.entity.DomainDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDomainDO;
import com.xiaoniucode.etp.server.web.param.domain.DomainBatchDeleteParam;
import com.xiaoniucode.etp.server.web.param.domain.DomainCreateParam;
import com.xiaoniucode.etp.server.web.param.domain.DomainUpdateParam;
import com.xiaoniucode.etp.server.web.repository.DomainRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyDomainRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyRepository;
import com.xiaoniucode.etp.server.web.service.DomainService;
import com.xiaoniucode.etp.server.web.service.converter.DomainConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DomainServiceImpl implements DomainService {
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private DomainConvert domainConvert;
    @Autowired
    private DomainConfigService domainConfigService;

    @Override
    public PageResult<DomainDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DomainDO> resultPage = domainRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<DomainDTO> dtoList = domainConvert.toDTOList(resultPage.getContent());
        return PageResult.wrap(resultPage, dtoList);
    }

    @Override
    public PageResult<UsedDomainDTO> findUsedByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<ProxyDomainDO> resultPage = proxyDomainRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }

        List<String> proxyIds = resultPage.getContent().stream()
                .map(ProxyDomainDO::getProxyId)
                .distinct()
                .toList();
        Map<String, ProxyDO> proxyMap = proxyRepository.findAllById(proxyIds).stream()
                .collect(Collectors.toMap(ProxyDO::getId, Function.identity()));

        List<UsedDomainDTO> dtoList = resultPage.getContent().stream().map(domainDO -> {
            UsedDomainDTO dto = new UsedDomainDTO();
            dto.setId(domainDO.getId());
            dto.setFullDomain(domainDO.getFullDomain());
            dto.setDomain(domainDO.getDomain());
            dto.setRootDomain(domainDO.getRootDomain());
            dto.setDomainType(domainDO.getDomainType() != null ? domainDO.getDomainType().getCode() : null);
            dto.setProxyId(domainDO.getProxyId());
            ProxyDO proxyDO = proxyMap.get(domainDO.getProxyId());
            if (proxyDO != null) {
                dto.setProxyName(proxyDO.getName());
            }
            return dto;
        }).toList();

        return PageResult.wrap(resultPage, dtoList);
    }

    @Override
    public DomainDTO getById(Integer id) {
        DomainDO domainDO = domainRepository.findById(id)
                .orElseThrow(() -> new BizException("域名不存在"));
        return domainConvert.toDTO(domainDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DomainDTO create(DomainCreateParam param) {
        if (domainRepository.existsByDomain(param.getDomain())) {
            throw new BizException("根域名已存在: " + param.getDomain());
        }
        DomainDO domainDO = new DomainDO();
        domainDO.setDomain(param.getDomain());
        domainDO.setRemark(param.getRemark());
        DomainDO saved = domainRepository.save(domainDO);
        domainConfigService.evictBaseDomains();
        return domainConvert.toDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DomainUpdateParam param) {
        DomainDO domainDO = domainRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("域名不存在"));
        domainDO.setRemark(param.getRemark());
        domainRepository.save(domainDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(DomainBatchDeleteParam param) {
        List<Integer> ids = param.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        domainRepository.deleteAllById(ids);
        domainConfigService.evictBaseDomains();
    }
}
