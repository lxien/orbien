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
package io.github.lxien.orbien.server.web.dto.proxy;

import lombok.Data;

import java.io.Serializable;

/**
 * 列表页瞬时上下行速率
 */
@Data
public class ProxyTrafficSnippetDTO implements Serializable {
    /**
     * 上行速率（字节/秒）
     */
    private double upRate;
    /**
     * 下行速率（字节/秒）
     */
    private double downRate;
}
