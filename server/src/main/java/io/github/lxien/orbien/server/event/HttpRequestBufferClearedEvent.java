package io.github.lxien.orbien.server.event;

import io.github.lxien.orbien.server.notify.Event;
import lombok.Getter;

@Getter
public class HttpRequestBufferClearedEvent extends Event {
    private final String proxyId;

    public HttpRequestBufferClearedEvent(String proxyId) {
        this.proxyId = proxyId;
    }
}
