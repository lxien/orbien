package io.github.lxien.orbien.server.event;

import io.github.lxien.orbien.server.notify.Event;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import lombok.Getter;

@Getter
public class HttpRequestCapturedEvent extends Event {
    private final HttpCaptureRecord record;

    public HttpRequestCapturedEvent(HttpCaptureRecord record) {
        this.record = record;
    }
}
