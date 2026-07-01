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

package com.xiaoniucode.etp.server.web.support.portpool;

import com.xiaoniucode.etp.server.web.common.exception.BizException;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PortPoolParser {

    private static final Pattern PORT_PATTERN = Pattern.compile("^(\\d+)(?:-(\\d+))?$");

    private PortPoolParser() {
    }

    public record ParsedPort(int portStart, Integer portEnd) {
        public boolean isRange() {
            return portEnd != null;
        }
    }

    public static ParsedPort parse(String port) {
        if (!StringUtils.hasText(port)) {
            throw new BizException("端口不能为空");
        }
        Matcher matcher = PORT_PATTERN.matcher(port.trim());
        if (!matcher.matches()) {
            throw new BizException("端口格式无效，请输入单个端口或范围端口，如 8000 或 8000-9000");
        }
        int portStart = parsePortValue(matcher.group(1));
        String endText = matcher.group(2);
        if (endText == null) {
            return new ParsedPort(portStart, null);
        }
        int portEnd = parsePortValue(endText);
        if (portEnd <= portStart) {
            throw new BizException("范围端口结束值必须大于起始值");
        }
        return new ParsedPort(portStart, portEnd);
    }

    private static int parsePortValue(String value) {
        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new BizException("端口号格式无效");
        }
        if (port < 1 || port > 65535) {
            throw new BizException("端口号必须在 1-65535 之间");
        }
        return port;
    }
}
