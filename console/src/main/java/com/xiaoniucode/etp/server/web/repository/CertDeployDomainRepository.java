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

package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.CertDeployDomainDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertDeployDomainRepository extends JpaRepository<CertDeployDomainDO, Long> {

    List<CertDeployDomainDO> findByDeployId(Long deployId);

    void deleteByDomain(String domain);

    void deleteByDeployId(Long deployId);

    List<CertDeployDomainDO> findByDeployIdIn(List<Long> deployIds);

    void deleteByDeployIdIn(List<Long> deployIds);
}
