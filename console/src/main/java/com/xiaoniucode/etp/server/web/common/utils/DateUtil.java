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

package com.xiaoniucode.etp.server.web.common.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日期工具类
 *
 * @author xiaoniucode
 */
public class DateUtil {

    /**
     * 将 java.util.Date 转换为 java.time.LocalDate
     * 使用系统默认时区
     *
     * @param date Date 对象，可为 null
     * @return LocalDate 对象，若输入为 null 则返回 null
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}