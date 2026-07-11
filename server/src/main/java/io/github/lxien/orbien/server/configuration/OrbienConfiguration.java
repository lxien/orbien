package io.github.lxien.orbien.server.configuration;


import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.uid.impl.CachedUidGenerator;
import io.github.lxien.orbien.server.notify.EventBus;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

@Configuration
public class OrbienConfiguration {
    @Bean
    public EventBus eventBus() {
        return new EventBus(16384);
    }

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    @Bean(destroyMethod = "stop")
    public HashedWheelTimer hashedWheelTimer() {
        return new HashedWheelTimer(
                new DefaultThreadFactory("wheel-timer"),
                100,
                TimeUnit.MILLISECONDS,
                512,
                true
        );
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 19位雪花算法生成，单机模式
     *
     * @return ID
     */
    @Bean
    @ConditionalOnMissingBean(UidGenerator.class)
    public UidGenerator uidGenerator() {
        CachedUidGenerator generator = new CachedUidGenerator();
        generator.setTimeBits(29);
        generator.setWorkerBits(21);
        generator.setSeqBits(13);
        generator.setEpochStr("2026-04-14");
        generator.setWorkerIdAssigner(() -> 1L);
        return generator;
    }
}
