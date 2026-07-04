package io.github.lxien.orbien.server.exceptions;

public class PortConflictException extends OrbienException {
    public PortConflictException(int port) {
        super("端口已被占用: " + port);
    }
}

