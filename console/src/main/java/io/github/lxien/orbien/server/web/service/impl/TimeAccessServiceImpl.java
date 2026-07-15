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

import io.github.lxien.orbien.core.domain.TimeAccessWindow;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.time.TimeAccessSupport;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.timeaccess.TimeAccessDetailDTO;
import io.github.lxien.orbien.server.web.entity.TimeAccessDO;
import io.github.lxien.orbien.server.web.entity.TimeAccessWindowDO;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessUpdateParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowAddParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowUpdateParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.TimeAccessRepository;
import io.github.lxien.orbien.server.web.repository.TimeAccessWindowRepository;
import io.github.lxien.orbien.server.web.service.TimeAccessService;
import io.github.lxien.orbien.server.web.service.converter.TimeAccessConvert;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class TimeAccessServiceImpl implements TimeAccessService {
    @Autowired
    private TimeAccessRepository timeAccessRepository;
    @Autowired
    private TimeAccessWindowRepository timeAccessWindowRepository;
    @Autowired
    private TimeAccessConvert timeAccessConvert;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TimeAccessDetailDTO getByProxyId(String proxyId) {
        TimeAccessDO accessDO = ensureParent(proxyId);
        List<TimeAccessWindowDO> windows = timeAccessWindowRepository.findByProxyIdOrderByIdAsc(proxyId);
        return timeAccessConvert.toDetailDTO(accessDO, windows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TimeAccessUpdateParam param) {
        TimeAccessDO accessDO = ensureParent(param.getProxyId());
        accessDO.setEnabled(param.getEnabled());
        accessDO.setMode(AccessControl.fromCode(param.getMode()));
        accessDO.setTimeEnabled(param.getTimeEnabled());

        String timezone = StringUtils.hasText(param.getTimezone())
                ? param.getTimezone().trim()
                : TimeAccessSupport.DEFAULT_TIMEZONE;
        try {
            TimeAccessSupport.validateTimezone(timezone);
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage());
        }
        accessDO.setTimezone(timezone);

        try {
            TimeAccessSupport.validateDays(param.getDays() == null ? null : new HashSet<>(param.getDays()));
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage());
        }
        accessDO.setDaysMask(TimeAccessSupport.toDaysMask(
                param.getDays() == null ? null : new HashSet<>(param.getDays())));

        timeAccessRepository.save(accessDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWindow(TimeAccessWindowAddParam param) {
        ensureParent(param.getProxyId());
        if (timeAccessWindowRepository.countByProxyId(param.getProxyId()) >= TimeAccessSupport.MAX_WINDOWS) {
            throw new BizException("时间窗数量超过限制: " + TimeAccessSupport.MAX_WINDOWS);
        }
        TimeAccessWindowDO windowDO = timeAccessConvert.toWindowDO(param);
        normalizeWindow(windowDO);
        timeAccessWindowRepository.save(windowDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWindow(TimeAccessWindowUpdateParam param) {
        TimeAccessWindowDO windowDO = timeAccessWindowRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("时间窗不存在"));
        if (!Objects.equals(windowDO.getProxyId(), param.getProxyId())) {
            throw new BizException("代理 ID 不匹配");
        }
        timeAccessConvert.updateWindowDO(windowDO, param);
        normalizeWindow(windowDO);
        timeAccessWindowRepository.save(windowDO);
        scheduleRefresh(param.getProxyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWindow(Long id) {
        TimeAccessWindowDO windowDO = timeAccessWindowRepository.findById(id)
                .orElseThrow(() -> new BizException("时间窗不存在"));
        String proxyId = windowDO.getProxyId();
        timeAccessWindowRepository.deleteById(id);
        scheduleRefresh(proxyId);
    }

    private TimeAccessDO ensureParent(String proxyId) {
        return timeAccessRepository.findById(proxyId)
                .orElseGet(() -> timeAccessRepository.save(new TimeAccessDO(proxyId)));
    }

    private void normalizeWindow(TimeAccessWindowDO windowDO) {
        try {
            TimeAccessWindow window = new TimeAccessWindow(windowDO.getStartTime(), windowDO.getEndTime());
            TimeAccessSupport.validateWindow(window);
            windowDO.setStartTime(window.getStart());
            windowDO.setEndTime(window.getEnd());
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage());
        }
    }

    private void scheduleRefresh(String proxyId) {
        transactionHelper.afterCommit(() -> proxyRuntimeSyncService.refreshServerEntryPolicy(proxyId));
    }
}
