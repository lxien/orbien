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

package com.xiaoniucode.etp.server.event;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.core.notify.Event;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import lombok.Getter;

import java.util.Set;

@Getter
public class ProxyReportEvent extends Event {
    /**
     * 最新的代理配置信息
     */
    private final ProxyConfig proxyConfig;
    /**
     * 基础域名，用于子域名
     */
    private String baseDomain;
    /**
     * 域名列表
     */
    private Set<DomainInfo> domains;
    /**
     * 是否是更新事件，true=更新事件，false=新增事件
     */
    private final boolean isUpdate;
    /**
     * 更新操作，是否发生数据变化
     */
    private final boolean hasChange;


    public DomainType getDomainType(){
        return proxyConfig.getRouteConfig().getDomainType();
    }
    public ProxyReportEvent(boolean isUpdate, ProxyConfig proxyConfig,boolean hasChange) {
        this.isUpdate = isUpdate;
        this.proxyConfig = proxyConfig;
        this.hasChange=hasChange;
    }

    public ProxyReportEvent(boolean isUpdate, String baseDomain, Set<DomainInfo> domains, ProxyConfig proxyConfig, boolean hasChange) {
        this.isUpdate = isUpdate;
        this.proxyConfig = proxyConfig;
        this.baseDomain = baseDomain;
        this.domains = domains;
        this.hasChange=hasChange;
    }
}
