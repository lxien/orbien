package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.server.Lifecycle;

public interface TransportListener extends Lifecycle {

    TransportProtocol protocol();

    void bind(TransportBindOptions options, SessionAcceptor acceptor);
}
