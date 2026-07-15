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
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.time.TimeAccessSupport;
import io.github.lxien.orbien.server.web.dto.timeaccess.TimeAccessDetailDTO;
import io.github.lxien.orbien.server.web.dto.timeaccess.TimeAccessWindowDTO;
import io.github.lxien.orbien.server.web.entity.TimeAccessDO;
import io.github.lxien.orbien.server.web.entity.TimeAccessWindowDO;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowAddParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowUpdateParam;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TimeAccessConvert {

    default TimeAccessDetailDTO toDetailDTO(TimeAccessDO accessDO, List<TimeAccessWindowDO> windows) {
        TimeAccessDetailDTO dto = new TimeAccessDetailDTO();
        dto.setProxyId(accessDO.getProxyId());
        dto.setEnabled(Boolean.TRUE.equals(accessDO.getEnabled()));
        dto.setMode(accessDO.getMode() == null ? AccessControl.ALLOW.getCode() : accessDO.getMode().getCode());
        dto.setTimeEnabled(accessDO.getTimeEnabled() == null || Boolean.TRUE.equals(accessDO.getTimeEnabled()));
        dto.setTimezone(accessDO.getTimezone());
        dto.setDays(new ArrayList<>(TimeAccessSupport.fromDaysMask(
                accessDO.getDaysMask() == null ? 0 : accessDO.getDaysMask())));
        dto.setWindows(toWindowDTOs(windows));
        return dto;
    }

    default List<TimeAccessWindowDTO> toWindowDTOs(List<TimeAccessWindowDO> windows) {
        if (windows == null || windows.isEmpty()) {
            return List.of();
        }
        List<TimeAccessWindowDTO> result = new ArrayList<>(windows.size());
        for (TimeAccessWindowDO window : windows) {
            TimeAccessWindowDTO dto = new TimeAccessWindowDTO();
            dto.setId(window.getId());
            dto.setStart(window.getStartTime());
            dto.setEnd(window.getEndTime());
            result.add(dto);
        }
        return result;
    }

    default TimeAccessWindowDO toWindowDO(TimeAccessWindowAddParam param) {
        TimeAccessWindowDO windowDO = new TimeAccessWindowDO();
        windowDO.setProxyId(param.getProxyId());
        windowDO.setStartTime(param.getStart());
        windowDO.setEndTime(param.getEnd());
        return windowDO;
    }

    default void updateWindowDO(TimeAccessWindowDO windowDO, TimeAccessWindowUpdateParam param) {
        windowDO.setStartTime(param.getStart());
        windowDO.setEndTime(param.getEnd());
    }
}
