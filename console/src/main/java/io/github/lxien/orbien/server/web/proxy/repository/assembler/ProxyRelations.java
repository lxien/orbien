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

package io.github.lxien.orbien.server.web.proxy.repository.assembler;

import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;

import java.util.List;

/**
 * 代理关联表聚合数据，用于组装 {@link ProxyConfigExt}。
 */
public record ProxyRelations(
        List<ProxyTargetDO> targets,
        List<AccessControlRuleDO> accessControlRules,
        List<ProxyDomainDO> domains,
        List<BasicUserDO> basicUsers,
        HealthCheckDO healthCheck,
        Socks5AuthDO socks5Auth,
        List<Socks5UserDO> socks5Users,
        FileShareAuthDO fileShareAuth,
        List<FileShareUserDO> fileShareUsers,
        FileShareLimitsDO fileShareLimits,
        HeaderRewriteDO headerRewrite,
        List<HeaderRewriteRuleDO> headerRewriteRules
) {
    public static ProxyRelations empty() {
        return new ProxyRelations(List.of(), List.of(), List.of(), List.of(), null, null, List.of(),
                null, List.of(), null, null, List.of());
    }
}
