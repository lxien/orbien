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

import io.github.lxien.orbien.core.enums.PortPoolType;
import io.github.lxien.orbien.server.port.PortPoolManager;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.portpool.PortPoolDTO;
import io.github.lxien.orbien.server.web.entity.PortPoolDO;
import io.github.lxien.orbien.server.web.param.portpool.PortPoolBatchDeleteParam;
import io.github.lxien.orbien.server.web.param.portpool.PortPoolCreateParam;
import io.github.lxien.orbien.server.web.param.portpool.PortPoolUpdateParam;
import io.github.lxien.orbien.server.web.repository.PortPoolRepository;
import io.github.lxien.orbien.server.web.service.PortPoolService;
import io.github.lxien.orbien.server.web.service.PortPoolSyncService;
import io.github.lxien.orbien.server.web.service.converter.PortPoolConvert;
import io.github.lxien.orbien.server.web.support.portpool.PortPoolParser;
import io.github.lxien.orbien.server.web.support.portpool.PortPoolParser.ParsedPort;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class PortPoolServiceImpl implements PortPoolService {
    @Autowired
    private PortPoolRepository portPoolRepository;
    @Autowired
    private PortPoolConvert portPoolConvert;
    @Autowired
    private PortPoolSyncService portPoolSyncService;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private PortPoolManager portPoolManager;

    private static final int SUGGEST_MIN = 1;
    private static final int SUGGEST_MAX = 10;
    private static final int SUGGEST_DEFAULT = 5;

    @Override
    public PageResult<PortPoolDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PortPoolDO> resultPage = portPoolRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<PortPoolDTO> dtoList = portPoolConvert.toDTOList(resultPage.getContent());
        return PageResult.wrap(resultPage, dtoList);
    }

    @Override
    public PortPoolDTO getById(Long id) {
        PortPoolDO poolDO = portPoolRepository.findById(id)
                .orElseThrow(() -> new BizException("端口配置不存在"));
        return portPoolConvert.toDTO(poolDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortPoolDTO create(PortPoolCreateParam param) {
        PortPoolType type = PortPoolType.fromCode(param.getType());
        ParsedPort parsedPort = PortPoolParser.parse(param.getPort());
        validateDuplicate(type, parsedPort.startPort(), parsedPort.endPort(), null);

        PortPoolDO poolDO = new PortPoolDO();
        applyPort(poolDO, parsedPort);
        poolDO.setType(type);
        poolDO.setRemark(param.getRemark());
        PortPoolDO saved = portPoolRepository.save(poolDO);
        transactionHelper.afterCommit(() -> portPoolSyncService.onCreated(saved));
        return portPoolConvert.toDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PortPoolUpdateParam param) {
        PortPoolDO entity = portPoolRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("端口配置不存在"));
        PortPoolDO before = copyForValidate(entity);

        PortPoolType type = PortPoolType.fromCode(param.getType());
        ParsedPort parsedPort = PortPoolParser.parse(param.getPort());
        validateDuplicate(type, parsedPort.startPort(), parsedPort.endPort(), param.getId());

        PortPoolDO after = copyForValidate(entity);
        applyPort(after, parsedPort);
        after.setType(type);
        after.setRemark(param.getRemark());
        portPoolSyncService.validateUpdate(before, after);

        applyPort(entity, parsedPort);
        entity.setType(type);
        entity.setRemark(param.getRemark());
        PortPoolDO saved = portPoolRepository.save(entity);
        PortPoolDO beforeSnapshot = before;
        transactionHelper.afterCommit(() -> portPoolSyncService.onUpdated(beforeSnapshot, saved));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(PortPoolBatchDeleteParam param) {
        List<Long> ids = param.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<PortPoolDO> toDelete = portPoolRepository.findAllById(ids);
        for (PortPoolDO entity : toDelete) {
            portPoolSyncService.validateRemovable(entity);
        }
        portPoolRepository.deleteAllById(ids);
        transactionHelper.afterCommit(() -> toDelete.forEach(portPoolSyncService::onDeleted));
    }

    private PortPoolDO copyForValidate(PortPoolDO source) {
        PortPoolDO copy = new PortPoolDO();
        copy.setId(source.getId());
        copy.setType(source.getType());
        copy.setStartPort(source.getStartPort());
        copy.setEndPort(source.getEndPort());
        return copy;
    }

    private void applyPort(PortPoolDO poolDO, ParsedPort parsedPort) {
        poolDO.setStartPort(parsedPort.startPort());
        poolDO.setEndPort(parsedPort.endPort());
    }

    @Override
    public List<Integer> suggestAvailable(Integer type, Integer limit) {
        PortPoolType poolType = PortPoolType.fromCode(type);
        int size = limit == null ? SUGGEST_DEFAULT : limit;
        if (size < SUGGEST_MIN) {
            size = SUGGEST_MIN;
        }
        if (size > SUGGEST_MAX) {
            size = SUGGEST_MAX;
        }
        return portPoolManager.suggestAvailable(poolType, size);
    }

    private void validateDuplicate(PortPoolType type, Integer startPort, Integer endPort, Long excludeId) {
        boolean exists = excludeId == null
                ? portPoolRepository.existsByTypeAndStartPortAndEndPort(type, startPort, endPort)
                : portPoolRepository.existsByTypeAndStartPortAndEndPortAndIdNot(type, startPort, endPort, excludeId);
        if (exists) {
            throw new BizException("该协议下相同端口配置已存在");
        }
    }
}
