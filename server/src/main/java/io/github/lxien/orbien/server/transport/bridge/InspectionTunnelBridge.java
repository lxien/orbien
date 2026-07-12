package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.AbstractTunnelBridgeDecorator;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.netty.buffer.ByteBuf;

/**
 * Inspector 旁路抓包：读取 {@link ByteBuf} 副本用于展示，不改变转发语义
 */
public class InspectionTunnelBridge extends AbstractTunnelBridgeDecorator {
    private final HttpStreamCapture capture;

    public InspectionTunnelBridge(TunnelBridge delegate, HttpStreamCapture capture) {
        super(delegate);
        this.capture = capture;
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        if (capture != null) {
            capture.appendRequestBody(payload);
        }
        delegate.forwardToLocal(payload, sharedWithInbound);
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (capture != null) {
            capture.appendResponseBody(payload);
        }
        delegate.forwardToRemote(payload, sharedWithInbound);
    }
}
