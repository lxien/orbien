package io.github.lxien.orbien.server.inspector.replay;

import lombok.Getter;

@Getter
public class ReplayException extends RuntimeException {
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_CONFLICT = 409;
    public static final int CODE_UNPROCESSABLE = 422;
    public static final int CODE_TIMEOUT = 504;
    public static final int CODE_FAILED = 500;

    private final int code;
    private final ReplayStatus status;

    public ReplayException(int code, ReplayStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ReplayException badRequest(String message) {
        return new ReplayException(CODE_BAD_REQUEST, ReplayStatus.REJECTED, message);
    }

    public static ReplayException notFound(String message) {
        return new ReplayException(CODE_NOT_FOUND, ReplayStatus.REJECTED, message);
    }

    public static ReplayException conflict(String message) {
        return new ReplayException(CODE_CONFLICT, ReplayStatus.AGENT_OFFLINE, message);
    }

    public static ReplayException unprocessable(String message) {
        return new ReplayException(CODE_UNPROCESSABLE, ReplayStatus.REJECTED, message);
    }

    public static ReplayException timeout(String message) {
        return new ReplayException(CODE_TIMEOUT, ReplayStatus.TIMEOUT, message);
    }

    public static ReplayException failed(String message) {
        return new ReplayException(CODE_FAILED, ReplayStatus.FAILED, message);
    }
}
