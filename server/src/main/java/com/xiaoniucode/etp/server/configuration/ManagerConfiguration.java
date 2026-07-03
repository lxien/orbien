package com.xiaoniucode.etp.server.configuration;

import com.xiaoniucode.etp.server.port.PortAcceptor;
import com.xiaoniucode.etp.server.port.UdpPortAcceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManagerConfiguration {

    @Bean
    public PortAcceptor portAcceptor() {
        return new PortAcceptor();
    }

    @Bean
    public UdpPortAcceptor udpPortAcceptor() {
        return new UdpPortAcceptor();
    }
}
