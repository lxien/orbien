package io.github.lxien.orbien.server.inspector;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.event.HttpRequestBufferClearedEvent;
import io.github.lxien.orbien.server.event.HttpRequestCapturedEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InspectorBuffer {
    private final Map<String, Deque<HttpCaptureRecord>> recordsByProxy = new ConcurrentHashMap<>();
    private final InspectorProperties properties;
    private final EventBus eventBus;

    @Autowired
    public InspectorBuffer(InspectorProperties properties, EventBus eventBus) {
        this.properties = properties;
        this.eventBus = eventBus;
    }

    public void append(HttpCaptureRecord record) {
        if (record == null || record.getProxyId() == null) {
            return;
        }
        Deque<HttpCaptureRecord> deque = recordsByProxy.computeIfAbsent(record.getProxyId(), key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(record);
            while (deque.size() > properties.getMaxRecordsPerProxy()) {
                deque.removeLast();
            }
        }
        eventBus.publishAsync(new HttpRequestCapturedEvent(record));
    }

    public List<HttpCaptureRecordSummary> listSummaries(String proxyId, int limit) {
        Deque<HttpCaptureRecord> deque = recordsByProxy.get(proxyId);
        if (deque == null) {
            return List.of();
        }
        int size = Math.max(1, Math.min(limit, properties.getMaxRecordsPerProxy()));
        List<HttpCaptureRecordSummary> result = new ArrayList<>();
        synchronized (deque) {
            int count = 0;
            for (HttpCaptureRecord record : deque) {
                result.add(record.toSummary());
                count++;
                if (count >= size) {
                    break;
                }
            }
        }
        return result;
    }

    public HttpCaptureRecord findById(String id) {
        if (id == null) {
            return null;
        }
        for (Deque<HttpCaptureRecord> deque : recordsByProxy.values()) {
            synchronized (deque) {
                for (HttpCaptureRecord record : deque) {
                    if (id.equals(record.getId())) {
                        return record;
                    }
                }
            }
        }
        return null;
    }

    public void clear(String proxyId) {
        if (proxyId == null) {
            return;
        }
        recordsByProxy.remove(proxyId);
        eventBus.publishAsync(new HttpRequestBufferClearedEvent(proxyId));
    }

    public static boolean shouldCapture(StreamContext context, InspectorProperties properties) {
        if (context == null || properties == null || !properties.isEnabled()) {
            return false;
        }
        if (context.getProtocol() == null || !context.getProtocol().isHttpOrHttps()) {
            return false;
        }
        ProxyConfig config = context.getProxyConfig();
        return config != null && config.isInspectorEnabled();
    }
}
