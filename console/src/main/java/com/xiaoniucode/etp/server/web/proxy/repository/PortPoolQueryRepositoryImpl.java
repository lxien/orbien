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

import com.xiaoniucode.etp.core.domain.PortInterval;
import com.xiaoniucode.etp.core.enums.PortPoolType;
import com.xiaoniucode.etp.core.utils.PortIntervalUtils;
import com.xiaoniucode.etp.server.service.repository.PortPoolQueryRepository;
import com.xiaoniucode.etp.server.web.entity.PortPoolDO;
import com.xiaoniucode.etp.server.web.repository.PortPoolRepository;
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
