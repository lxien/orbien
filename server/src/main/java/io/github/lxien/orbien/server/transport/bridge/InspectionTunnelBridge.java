package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AbstractTunnelBridgeDecorator;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;

/**
 * Inspector 旁路抓包，读取 {@link ByteBuf} 副本用于展示，不改变转发语义
 * <p>
 * 同一 keep-alive 连接上的后续请求也会被捕获
 */
public class InspectionTunnelBridge extends AbstractTunnelBridgeDecorator {
    private final StreamContext context;
    private final InspectorProperties properties;
    private final InspectorBuffer inspectorBuffer;
    private final ProxyConfigService proxyConfigService;
    private final Object captureLock = new Object();
    private volatile boolean lastCaptureDecision;
    private volatile long lastCaptureCheckAt;

    public InspectionTunnelBridge(TunnelBridge delegate,
                                  StreamContext context,
                                  InspectorProperties properties,
                                  InspectorBuffer inspectorBuffer,
                                  ProxyConfigService proxyConfigService) {
        super(delegate);
        this.context = context;
        this.properties = properties;
        this.inspectorBuffer = inspectorBuffer;
        this.proxyConfigService = proxyConfigService;
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        HttpStreamCapture capture = ensureCapture();
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
        HttpStreamCapture capture = ensureCapture();
        if (capture != null) {
            capture.appendResponseBody(payload);
        }
        delegate.forwardToRemote(payload, sharedWithInbound);
    }

    private HttpStreamCapture ensureCapture() {
        HttpStreamCapture existing = context.getHttpStreamCapture();
        if (existing != null) {
            return existing;
        }
        if (!liveShouldCapture()) {
            return null;
        }
        synchronized (captureLock) {
            existing = context.getHttpStreamCapture();
            if (existing != null) {
                return existing;
            }
            if (!liveShouldCapture()) {
                return null;
            }
            HttpStreamCapture capture = new HttpStreamCapture(context, properties);
            capture.setCompletionHandler(this::onCaptureComplete);
            context.setHttpStreamCapture(capture);
            return capture;
        }
    }

    private boolean liveShouldCapture() {
        long now = System.currentTimeMillis();
        if (now - lastCaptureCheckAt < 1000L) {
            return lastCaptureDecision;
        }
        boolean enabled = computeLiveShouldCapture();
        lastCaptureDecision = enabled;
        lastCaptureCheckAt = now;
        return enabled;
    }

    private boolean computeLiveShouldCapture() {
        if (context.isReplay()) {
            return context.getProtocol() != null && context.getProtocol().isHttpOrHttps();
        }
        if (properties == null || !properties.isEnabled()) {
            return false;
        }
        if (context.getProtocol() == null || !context.getProtocol().isHttpOrHttps()) {
            return false;
        }
        String proxyId = context.getProxyId();
        if (proxyId == null || proxyConfigService == null) {
            ProxyConfig config = context.getProxyConfig();
            return config != null && config.isInspectorEnabled();
        }
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            ProxyConfig config = context.getProxyConfig();
            return config != null && config.isInspectorEnabled();
        }
        return ext.getProxyConfig().isInspectorEnabled();
    }

    private void onCaptureComplete(HttpCaptureRecord record) {
        if (record == null) {
            return;
        }
        if (context.isReplay()) {
            // 重放流的 completion 由 StreamOpenResponseAction 注册的 handler 处理；
            // 此处仅兜底：若 lazy 创建的 capture 走到这里，仍需写入缓冲
            if (context.isReplayCaptureToBuffer() && inspectorBuffer != null) {
                inspectorBuffer.append(record);
            }
            var future = context.getReplayCompletion();
            if (future != null && !future.isDone()) {
                future.complete(record);
            }
            return;
        }
        if (inspectorBuffer != null) {
            inspectorBuffer.append(record);
        }
    }
}
