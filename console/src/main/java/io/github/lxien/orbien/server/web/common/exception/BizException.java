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

package io.github.lxien.orbien.server.web.common.exception;

import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {
    @Getter
    private final int code;
    private final String message;

    public BizException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public BizException(String message) {
        this.code = 1;
        this.message = message;
    }

    public static BizException of(String message) {
        return new BizException(message);
    }

    public static void throwIfNull(Object value, String msg) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new BizException(msg);
        }
    }


    @Override
    public String getMessage() {
        return message;
    }
}
