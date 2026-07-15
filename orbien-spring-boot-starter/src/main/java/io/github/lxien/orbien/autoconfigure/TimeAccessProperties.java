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

package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.time.TimeAccessSupport;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class TimeAccessProperties implements Serializable {
    private boolean enabled = false;
    private AccessControl mode = AccessControl.ALLOW;
    private boolean timeEnabled = true;
    private String timezone = TimeAccessSupport.DEFAULT_TIMEZONE;
    private Set<Integer> days = new LinkedHashSet<>();
    private List<Window> windows = new ArrayList<>();

    @Data
    public static class Window implements Serializable {
        private String start;
        private String end;
    }
}
