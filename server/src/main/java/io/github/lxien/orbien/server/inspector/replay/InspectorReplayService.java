package io.github.lxien.orbien.server.inspector.replay;

import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class InspectorReplayService {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(InspectorReplayService.class);

    private final InspectorBuffer inspectorBuffer;
    private final InspectorProperties inspectorProperties;
    private final AgentManager agentManager;
    private final ProxyConfigService proxyConfigService;
    private final ReplayStreamBootstrap replayStreamBootstrap;

    private Semaphore replaySemaphore;

    public InspectorReplayService(InspectorBuffer inspectorBuffer,
                                  InspectorProperties inspectorProperties,
                                  AgentManager agentManager,
                                  ProxyConfigService proxyConfigService,
                                  ReplayStreamBootstrap replayStreamBootstrap) {
        this.inspectorBuffer = inspectorBuffer;
        this.inspectorProperties = inspectorProperties;
        this.agentManager = agentManager;
        this.proxyConfigService = proxyConfigService;
        this.replayStreamBootstrap = replayStreamBootstrap;
    }

    @PostConstruct
    void init() {
        int permits = Math.max(1, inspectorProperties.getReplayMaxConcurrency());
        this.replaySemaphore = new Semaphore(permits);
    }

    public ReplayResult replay(String recordId, ReplayOptions options) {
        ReplayOptions opts = options != null ? options : ReplayOptions.builder().build();
        HttpCaptureRecord source = inspectorBuffer.findById(recordId);
        if (source == null) {
            throw ReplayException.notFound("请求记录不存在或已从缓冲中淘汰");
        }
        validateReplayable(source);
        ensureAgentOnline(source.getProxyId());

        boolean modified = opts.getOverrides() != null && !opts.getOverrides().isEmpty();
        long timeoutMs = opts.getTimeoutMs() > 0
                ? opts.getTimeoutMs()
                : inspectorProperties.getReplayTimeoutMs();
        timeoutMs = Math.min(Math.max(timeoutMs, 1_000L), 60_000L);

        boolean acquired;
        try {
            acquired = replaySemaphore.tryAcquire(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ReplayException.failed("重放被中断");
        }
        if (!acquired) {
            throw ReplayException.conflict("当前重放过多，请稍后重试");
        }

        StreamContext context = null;
        ByteBuf requestBuf = null;
        try {
            String scheme = StringUtils.hasText(source.getScheme()) ? source.getScheme() : "http";
            requestBuf = HttpRequestRebuilder.build(
                    source,
                    opts.getOverrides(),
                    ReplayStreamBootstrap.REPLAY_CLIENT_IP,
                    scheme,
                    source.getId()
            );

            CompletableFuture<HttpCaptureRecord> completion = new CompletableFuture<>();
            context = replayStreamBootstrap.start(
                    source,
                    requestBuf,
                    opts.isCaptureToBuffer(),
                    completion
            );
            requestBuf = null; // ownership transferred to stream / attr

            HttpCaptureRecord replayRecord = completion.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (replayRecord == null) {
                throw ReplayException.failed("重放未收到响应");
            }
            closeQuietly(context);
            replayStreamBootstrap.finish(context);
            context = null;
            return ReplayResult.builder()
                    .sourceRecordId(source.getId())
                    .replayRecordId(replayRecord.getId())
                    .modified(modified)
                    .status(ReplayStatus.SUCCESS)
                    .record(replayRecord)
                    .build();
        } catch (ReplayException ex) {
            closeQuietly(context);
            replayStreamBootstrap.finish(context);
            context = null;
            throw ex;
        } catch (TimeoutException ex) {
            closeQuietly(context);
            replayStreamBootstrap.finish(context);
            context = null;
            throw ReplayException.timeout("重放超时，请检查上游服务");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            closeQuietly(context);
            replayStreamBootstrap.finish(context);
            context = null;
            throw ReplayException.failed("重放被中断");
        } catch (Exception ex) {
            closeQuietly(context);
            replayStreamBootstrap.finish(context);
            context = null;
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            logger.warn("[Inspector][Replay] 重放失败 recordId={} msg={}", recordId, cause.getMessage());
            if (cause instanceof ReplayException replayException) {
                throw replayException;
            }
            throw ReplayException.failed("重放失败: " + cause.getMessage());
        } finally {
            if (requestBuf != null && requestBuf.refCnt() > 0) {
                requestBuf.release();
            }
            if (context != null) {
                closeQuietly(context);
                replayStreamBootstrap.finish(context);
            }
            replaySemaphore.release();
        }
    }

    private void validateReplayable(HttpCaptureRecord source) {
        if (source.isRequestBodyTruncated()) {
            throw ReplayException.badRequest("请求体已截断，无法重放");
        }
        if (!StringUtils.hasText(source.getMethod())) {
            throw ReplayException.badRequest("HTTP 方法未知，无法重放");
        }
        String method = source.getMethod().trim().toUpperCase(Locale.ROOT);
        if (!HttpRequestRebuilder.ALLOWED_METHODS.contains(method)) {
            throw ReplayException.badRequest("不支持的 HTTP 方法: " + method);
        }
        if (source.getStatus() == 101) {
            throw ReplayException.unprocessable("协议升级请求不支持重放");
        }
        if (hasUpgrade(source.getRequestHeaders()) || hasUpgrade(source.getResponseHeaders())) {
            throw ReplayException.unprocessable("协议升级请求不支持重放");
        }
        if (!StringUtils.hasText(source.getProxyId())) {
            throw ReplayException.badRequest("捕获记录缺少代理信息");
        }
    }

    private void ensureAgentOnline(String proxyId) {
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            throw ReplayException.conflict("代理不存在或未加载");
        }
        ProxyConfig config = ext.getProxyConfig();
        if (config.getStatus() != null && config.getStatus().isClosed()) {
            throw ReplayException.conflict("代理已关闭");
        }
        String agentId = config.getAgentId();
        if (!StringUtils.hasText(agentId) || !agentManager.isOnline(agentId)) {
            throw ReplayException.conflict("Agent 离线或无可用上游，请稍后重试");
        }
    }

    private static boolean hasUpgrade(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && "upgrade".equalsIgnoreCase(entry.getKey())) {
                return StringUtils.hasText(entry.getValue());
            }
        }
        return false;
    }

    private static void closeQuietly(StreamContext context) {
        if (context == null) {
            return;
        }
        try {
            var future = context.getReplayCompletion();
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new TimeoutException("replay aborted"));
            }
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
