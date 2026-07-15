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

import io.github.lxien.orbien.core.enums.AccessControl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 时间周期访问限制配置
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class TimeAccessConfig implements Serializable {
    @Setter
    private boolean enabled;
    @Setter
    private AccessControl mode = AccessControl.ALLOW;
    @Setter
    private boolean timeEnabled = true;
    @Setter
    private String timezone;
    private final Set<Integer> days = new LinkedHashSet<>();
    private final List<TimeAccessWindow> windows = new CopyOnWriteArrayList<>();

    public TimeAccessConfig(boolean enabled, AccessControl mode, boolean timeEnabled,
                            String timezone, Set<Integer> days, List<TimeAccessWindow> windows) {
        this.enabled = enabled;
        this.mode = mode != null ? mode : AccessControl.ALLOW;
        this.timeEnabled = timeEnabled;
        this.timezone = timezone;
        if (days != null) {
            this.days.addAll(days);
        }
        if (windows != null && !windows.isEmpty()) {
            this.windows.addAll(windows);
        }
    }

    public void setDays(Set<Integer> days) {
        this.days.clear();
        if (days != null) {
            this.days.addAll(days);
        }
    }

    public void addDay(Integer day) {
        if (day != null) {
            this.days.add(day);
        }
    }

    public void addWindow(TimeAccessWindow window) {
        if (window != null) {
            this.windows.add(window);
        }
    }

    public void addWindows(List<TimeAccessWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            return;
        }
        this.windows.addAll(windows);
    }

    public Set<Integer> getDaysView() {
        return Collections.unmodifiableSet(days);
    }

    public List<TimeAccessWindow> getWindowsView() {
        return Collections.unmodifiableList(windows);
    }

    public boolean hasDays() {
        return !days.isEmpty();
    }

    public boolean hasWindows() {
        return !windows.isEmpty();
    }
}
