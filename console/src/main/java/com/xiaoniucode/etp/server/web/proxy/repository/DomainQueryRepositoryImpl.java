/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.web.proxy.repository;

import com.xiaoniucode.etp.server.service.repository.DomainQueryRepository;
import com.xiaoniucode.etp.server.web.entity.DomainDO;
import com.xiaoniucode.etp.server.web.repository.DomainRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyDomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DomainQueryRepositoryImpl implements DomainQueryRepository {
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;

    @Override
    public boolean existsByFullDomain(String fullDomain) {
        return proxyDomainRepository.existsByFullDomain(fullDomain);
    }

    @Override
    public List<String> findAllBaseDomains() {
        return domainRepository.findAll().stream().map(DomainDO::getDomain).toList();
    }
}
