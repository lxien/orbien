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

package io.github.lxien.orbien.server.web.listener;

import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.notify.EventListener;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.web.entity.DomainDO;
import io.github.lxien.orbien.server.web.repository.DomainRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DomainSynchronizer implements EventListener<TunnelServerBindEvent> {
    private final Logger logger = LoggerFactory.getLogger(DomainSynchronizer.class);
    @Autowired
    private EventBus eventBus;
    @Resource
    private AppConfig appConfig;
    @Autowired
    private DomainRepository domainRepository;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(TunnelServerBindEvent event) {
        Set<String> rootDomains = appConfig.getRootDomains();
        if (CollectionUtils.isEmpty(rootDomains)) {
            return;
        }
        // 一次性查询已存在的域名
        Set<DomainDO> existingDomains = domainRepository.findByDomainIn(rootDomains);

        //提取已存在的域名集合
        Set<String> existingDomainNames = existingDomains.stream()
                .map(DomainDO::getDomain)
                .collect(Collectors.toSet());

        // 筛选出需要新增的域名
        List<DomainDO> newDomains = rootDomains.stream()
                .filter(domain -> !existingDomainNames.contains(domain))
                .map(DomainDO::new)
                .collect(Collectors.toList());

        // 批量保存
        if (!newDomains.isEmpty()) {
            domainRepository.saveAll(newDomains);
            logger.info("同步域名到数据库, domains={}", newDomains.stream().map(DomainDO::getDomain).collect(Collectors.toList()));
        } else {
            logger.debug("所有域名已存在，跳过持久化");
        }
    }
}
