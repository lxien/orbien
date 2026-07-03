package com.xiaoniucode.etp.server.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DevModeInitializer {

    @Value("${etp.dev:false}")
    private boolean dev;

    @PostConstruct
    public void init() {
        if (dev) {
            System.setProperty("io.netty.leakDetection.targetRecords", "50");
            System.setProperty("io.netty.leakDetection.samplingRate", "1");
            System.setProperty("io.netty.leakDetection.level", "PARANOID");
            log.debug("Netty leak detection enabled");
        }
    }
}