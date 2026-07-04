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
package io.github.lxien.orbien.server.web.param.bandwidth;

import io.github.lxien.orbien.core.enums.BandwidthUnit;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import lombok.Data;

import java.io.Serializable;

@Data
public class BandwidthSaveParam implements Serializable {
    private Long limitTotal;
    private Long limitIn;
    private Long limitOut;
    /**
     * 单位
     */
    @EnumValue(enumClass = BandwidthUnit.class)
    private String unit;
    public void valid() {
        boolean hasBandwidth = limitTotal != null || limitIn != null || limitOut != null;

        if (limitTotal != null && limitTotal <= 0) {
            throw new BizException("总带宽限制必须大于 0");
        }

        if (limitIn != null && limitIn < 0) {
            throw new BizException("入站带宽限制必须大于等于 0");
        }

        if (limitOut != null && limitOut < 0) {
            throw new BizException("出站带宽限制必须大于等于 0");
        }

        if (hasBandwidth && unit == null) {
            throw new BizException("带宽单位不能为空");
        }

        if (limitTotal != null) {
            if (limitIn != null && limitIn > limitTotal) {
                throw new BizException("入站带宽限制不能大于总带宽限制");
            }
            if (limitOut != null && limitOut > limitTotal) {
                throw new BizException("出站带宽限制不能大于总带宽限制");
            }
            if (limitIn != null && limitOut != null && limitIn + limitOut > limitTotal) {
                throw new BizException("入站带宽限制与出站带宽限制之和不能大于总带宽限制");
            }
        }
    }
}
