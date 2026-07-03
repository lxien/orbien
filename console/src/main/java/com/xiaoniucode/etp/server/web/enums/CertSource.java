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

package com.xiaoniucode.etp.server.web.enums;

import lombok.Getter;

@Getter
public enum CertSource {
    MANUAL(1, "手动上传"),
    ACME(2, "自动申请");

    private final Integer code;
    private final String description;

    CertSource(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CertSource fromCode(Integer code) {
        for (CertSource source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知证书来源: " + code);
    }
}
