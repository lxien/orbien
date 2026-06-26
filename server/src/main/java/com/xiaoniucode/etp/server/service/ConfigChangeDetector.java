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

package com.xiaoniucode.etp.server.service;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.springframework.stereotype.Component;

@Component
public class ConfigChangeDetector {
    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.SIMPLE)
            .build();

    public Diff detectChanges(ProxyConfig oldConfig, ProxyConfig newConfig) {
        return JAVERS.compare(oldConfig, newConfig);
    }
    public boolean hasChanges(ProxyConfig oldConfig, ProxyConfig newConfig) {
        return detectChanges(oldConfig, newConfig).hasChanges();
    }
}
