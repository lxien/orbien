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

package io.github.lxien.orbien.autoconfigure;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class PortHolder {

    private final AtomicInteger port = new AtomicInteger(0);
    private final Environment env;

    public PortHolder(Environment env) {
        this.env = env;
    }

    public int get() {
        return port.get();
    }

    /**
     * 早期获取（兼容 Boot 2/3/4）
     *
     * @param event
     */
    @EventListener
    public void onWebServerInit(Object event) {
        try {
            if (!event.getClass().getName().endsWith("WebServerInitializedEvent")) {
                return;
            }

            Object webServer = event.getClass()
                    .getMethod("getWebServer")
                    .invoke(event);

            int p = (int) webServer.getClass()
                    .getMethod("getPort")
                    .invoke(webServer);

            port.compareAndSet(0, p);

        } catch (Throwable ignored) {
        }
    }

    @EventListener
    public void onReady(ApplicationReadyEvent e) {
        if (port.get() != 0) {
            return;
        }
        String p = env.getProperty("local.server.port");
        if (p != null) {
            port.compareAndSet(0, Integer.parseInt(p));
        }
    }
}