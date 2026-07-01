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

package com.xiaoniucode.etp.server.web.proxy.repository.assembler;

import com.xiaoniucode.etp.server.web.entity.*;

import java.util.List;

/**
 * 代理关联表聚合数据，用于组装 {@link com.xiaoniucode.etp.core.domain.ProxyConfigExt}。
 */
public record ProxyRelations(
        List<ProxyTargetDO> targets,
        List<AccessControlRuleDO> accessControlRules,
        List<ProxyDomainDO> domains,
        List<BasicUserDO> basicUsers,
        HealthCheckDO healthCheck
) {
    public static ProxyRelations empty() {
        return new ProxyRelations(List.of(), List.of(), List.of(), List.of(), null);
    }
}
