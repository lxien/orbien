package io.github.lxien.orbien.server.web.listener;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.HttpRequestBufferClearedEvent;
import io.github.lxien.orbien.server.web.service.inspector.InspectorSseHub;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InspectorBufferClearedEventListener implements EventListener<HttpRequestBufferClearedEvent> {
    private final EventBus eventBus;
    private final InspectorSseHub inspectorSseHub;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(HttpRequestBufferClearedEvent event) {
        inspectorSseHub.publishBufferCleared(event.getProxyId());
    }
}
