/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.web.proxy.repository;

import io.github.lxien.orbien.core.domain.PortInterval;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.github.lxien.orbien.core.utils.PortIntervalUtils;
import io.github.lxien.orbien.server.service.repository.PortPoolQueryRepository;
import io.github.lxien.orbien.server.web.entity.PortPoolDO;
import io.github.lxien.orbien.server.web.repository.PortPoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PortPoolQueryRepositoryImpl implements PortPoolQueryRepository {
    @Autowired
    private PortPoolRepository portPoolRepository;

    @Override
    public List<Integer> findAllPorts(PortPoolType type) {
        List<PortInterval> poolIntervals = portPoolRepository.findByType(type).stream()
                .map(this::toInterval)
                .toList();
        return PortIntervalUtils.toPortList(poolIntervals);
    }

    private PortInterval toInterval(PortPoolDO poolDO) {
        return PortInterval.ofRange(poolDO.getStartPort(), poolDO.getEndPort());
    }
}
