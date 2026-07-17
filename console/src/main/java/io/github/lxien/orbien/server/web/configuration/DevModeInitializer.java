/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.web.configuration;

import io.github.lxien.orbien.server.web.config.ConsoleProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevModeInitializer {

    private final ConsoleProperties consoleProperties;

    @PostConstruct
    public void init() {
        if (consoleProperties.isDev()) {
            System.setProperty("io.netty.leakDetection.targetRecords", "50");
            System.setProperty("io.netty.leakDetection.samplingRate", "1");
            System.setProperty("io.netty.leakDetection.level", "PARANOID");
            log.debug("Netty leak detection enabled");
        }
    }
}
