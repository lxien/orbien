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
package io.github.lxien.orbien.core.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@NoArgsConstructor
public class HeaderRewriteConfig implements Serializable {
    @Setter
    private boolean enabled;

    private final List<HeaderRewriteRule> requestRules = new CopyOnWriteArrayList<>();
    private final List<HeaderRewriteRule> responseRules = new CopyOnWriteArrayList<>();

    public HeaderRewriteConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public void addRequestRule(HeaderRewriteRule rule) {
        if (rule != null) {
            requestRules.add(rule);
        }
    }

    public void addResponseRule(HeaderRewriteRule rule) {
        if (rule != null) {
            responseRules.add(rule);
        }
    }

    public void addRequestRules(List<HeaderRewriteRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        requestRules.addAll(rules);
    }

    public void addResponseRules(List<HeaderRewriteRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        responseRules.addAll(rules);
    }

    public List<HeaderRewriteRule> getRequestRulesView() {
        return Collections.unmodifiableList(requestRules);
    }

    public List<HeaderRewriteRule> getResponseRulesView() {
        return Collections.unmodifiableList(responseRules);
    }

    public boolean hasRequestRules() {
        return enabled && !requestRules.isEmpty();
    }

    public boolean hasResponseRules() {
        return enabled && !responseRules.isEmpty();
    }
}
