package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.util.internal.logging.InternalLogger;

/**
 * 流转发失败时的统一止血逻辑：先停读，再关流（幂等）
 */
final class StreamForwardHelper {

    private StreamForwardHelper() {
    }

    static boolean shouldForward(StreamContext streamContext) {
        return streamContext != null && streamContext.canAcceptVisitorData();
    }

    static void abortAndClose(StreamContext streamContext, InternalLogger logger, String message, Throwable cause) {
        if (streamContext == null || streamContext.isLocalForwardingAborted()) {
            return;
        }
        streamContext.abortLocalForwarding();
        if (logger != null && message != null) {
            if (cause != null) {
                logger.warn(message, cause);
            } else {
                logger.warn(message);
            }
        }
        StreamState state = streamContext.getState();
        if (state != StreamState.CLOSED && state != StreamState.FAILED) {
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        }
    }
}
