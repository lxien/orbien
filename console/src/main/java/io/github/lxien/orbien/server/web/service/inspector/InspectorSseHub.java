package io.github.lxien.orbien.server.web.service.inspector;

import io.github.lxien.orbien.server.inspector.HttpCaptureRecordSummary;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class InspectorSseHub {
    private static final Logger log = LoggerFactory.getLogger(InspectorSseHub.class);

    private final InspectorRecordConverter recordConverter;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String proxyId) {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(proxyId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> detach(proxyId, emitter));
        emitter.onTimeout(() -> detach(proxyId, emitter));
        emitter.onError(ex -> detach(proxyId, emitter));
        if (!safeSend(emitter, "connected", "ok")) {
            detach(proxyId, emitter);
        }
        return emitter;
    }

    public void publishCaptured(HttpCaptureRecordSummary summary) {
        if (summary == null || summary.getProxyId() == null) {
            return;
        }
        HttpCaptureRecordSummaryDTO dto = recordConverter.toSummaryDto(summary);
        broadcast(summary.getProxyId(), "request.captured", dto);
    }

    public void publishBufferCleared(String proxyId) {
        broadcast(proxyId, "buffer.cleared", Map.of("proxyId", proxyId));
    }

    private void broadcast(String proxyId, String eventName, Object payload) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(proxyId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            if (!safeSend(emitter, eventName, payload)) {
                detach(proxyId, emitter);
            }
        }
    }

    private boolean safeSend(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
            return true;
        } catch (IOException | IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("SSE 客户端已断开: {}", ex.getMessage());
            }
            return false;
        }
    }

    private void detach(String proxyId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(proxyId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.remove(proxyId, emitters);
        }
    }
}
