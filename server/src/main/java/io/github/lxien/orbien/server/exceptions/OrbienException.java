package io.github.lxien.orbien.server.exceptions;

import lombok.Getter;
public class OrbienException extends RuntimeException {
    @Getter
    private final String message;

    public OrbienException(String message) {
        super(message);
        this.message = message;
    }
}