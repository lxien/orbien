package io.github.lxien.orbien.server.configuration;

import io.github.lxien.orbien.server.port.PortAcceptor;
import io.github.lxien.orbien.server.port.UdpPortAcceptor;
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
