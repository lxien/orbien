/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.dto.metrics;
import java.io.Serializable;
import java.time.LocalDateTime;
public record MetricsDTO(
    String proxyId,
    long readBytes,
    long writeBytes,
    long readMessages,
    long writeMessages,
    LocalDateTime createdAt
) implements Serializable {
}