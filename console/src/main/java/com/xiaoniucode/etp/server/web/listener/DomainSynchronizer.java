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

package com.xiaoniucode.etp.server.web.listener;

import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.notify.EventListener;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.event.TunnelServerBindEvent;
import com.xiaoniucode.etp.server.web.entity.DomainDO;
import com.xiaoniucode.etp.server.web.repository.DomainRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        String baseDomain = appConfig.getBaseDomain();
        if (!StringUtils.hasText(baseDomain)) {
            return;
        }
        boolean exists = domainRepository.existsByDomain(baseDomain);
        if (!exists) {
            domainRepository.save(new DomainDO(baseDomain));
            logger.info("同步域名到数据库, domain={}", baseDomain);
        } else {
            logger.debug("域名 {} 已经存在，跳过持久化", baseDomain);
        }
    }
}
