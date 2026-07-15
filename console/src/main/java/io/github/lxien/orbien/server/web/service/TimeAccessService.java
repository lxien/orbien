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
package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.timeaccess.TimeAccessDetailDTO;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessUpdateParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowAddParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowUpdateParam;

public interface TimeAccessService {
    TimeAccessDetailDTO getByProxyId(String proxyId);

    void update(TimeAccessUpdateParam param);

    void addWindow(TimeAccessWindowAddParam param);

    void updateWindow(TimeAccessWindowUpdateParam param);

    void deleteWindow(Long id);
}
