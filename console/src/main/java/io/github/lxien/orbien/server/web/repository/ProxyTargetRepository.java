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
package io.github.lxien.orbien.server.web.repository;
import io.github.lxien.orbien.server.web.entity.ProxyTargetDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 代理目标 Repository
 */
@Repository
public interface ProxyTargetRepository extends JpaRepository<ProxyTargetDO, Integer> {
    void deleteByProxyId(String proxyId);

    List<ProxyTargetDO> findByProxyId(String id);

    Optional<ProxyTargetDO> findFirstByProxyIdOrderByIdAsc(String proxyId);

    void deleteByProxyIdIn(List<String> ids);

    List<ProxyTargetDO> findByProxyIdIn(List<String> proxyIds);
}
