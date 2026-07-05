package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import java.util.concurrent.CompletableFuture;

public interface TransportConnector {

    TransportProtocol protocol();

    CompletableFuture<TransportSession> connect(TransportConnectOptions options);
}
