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

package com.xiaoniucode.etp.server.web;

import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.config.domain.DashboardConfig;
import com.xiaoniucode.etp.server.config.ConfigParser;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 服务启动入口
 *
 * @author xiaoniucode
 */
@EnableScheduling
@SpringBootApplication
@ComponentScan("com.xiaoniucode.etp")
public class TunnelServerStartup {
    public static void main(String[] args) {
        AppConfig config = ConfigParser.parse(args);
        DashboardConfig dashboard = config.getDashboard();
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TunnelServerStartup.class);
        if (Boolean.TRUE.equals(dashboard.getEnabled())) {
            builder.properties("spring.main.web-application-type=servlet", "server.port=" + dashboard.getPort());
        } else {
            builder.properties("spring.main.web-application-type=none");
        }

        builder.sources(TunnelServerStartup.class)
                .initializers(context ->
                        context.getBeanFactory().registerSingleton("appConfig", config))
                .run(args);

    }
}
