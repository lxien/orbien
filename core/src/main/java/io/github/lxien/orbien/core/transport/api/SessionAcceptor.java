package io.github.lxien.orbien.core.transport.api;

@FunctionalInterface
public interface SessionAcceptor {

    void accept(TransportSession session);
}
