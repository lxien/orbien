package io.github.lxien.orbien.server.web.listener;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.HttpRequestCapturedEvent;
import io.github.lxien.orbien.server.web.service.inspector.InspectorSseHub;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InspectorCapturedEventListener implements EventListener<HttpRequestCapturedEvent> {
    private final EventBus eventBus;
    private final InspectorSseHub inspectorSseHub;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(HttpRequestCapturedEvent event) {
        inspectorSseHub.publishCaptured(event.getRecord().toSummary());
    }
}
