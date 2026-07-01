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

import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.domain.DomainDTO;
import com.xiaoniucode.etp.server.web.dto.domain.UsedDomainDTO;
import com.xiaoniucode.etp.server.web.entity.DomainDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDomainDO;
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
            dto.setBaseDomain(domainDO.getBaseDomain());
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
        DomainDO domainDO = domainRepository.findById(id).orElse(null);
        if (domainDO == null) {
            return null;
        }
        return domainConvert.toDTO(domainDO);
    }
}
