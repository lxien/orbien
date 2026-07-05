package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.client.TunnelClient;
import io.github.lxien.orbien.client.config.AppConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class ClientBootstrap implements DisposableBean {
    private TunnelClient tunnelClient;
    private final PortHolder portHolder;
    private final Environment environment;
    private final OrbienClientProperties properties;
    private final ResourceLoader resourceLoader;
    private volatile boolean started = false;

    public ClientBootstrap(Environment environment,
                           OrbienClientProperties properties,
                           PortHolder portHolder,
                           ResourceLoader resourceLoader) {
        this.environment = environment;
        this.properties = properties;
        this.portHolder = portHolder;
        this.resourceLoader = resourceLoader;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        if (started) {
            return;
        }
        int port = portHolder.get();
        if (port <= 0) {
            throw new IllegalStateException("Cannot determine local server port");
        }
        String appName = environment.getProperty("spring.application.name", "unknown");
        startClient(appName, port);
    }

    private void startClient(String appName, int localPort) {
        AppConfig config = AppConfigBuilder.build(properties, resourceLoader, appName, localPort);
        tunnelClient = new TunnelClient(config);
        tunnelClient.start();
        started = true;
    }

    @Override
    public void destroy() {
        if (tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Exception ignored) {
            }
        }
        started = false;
    }
}
