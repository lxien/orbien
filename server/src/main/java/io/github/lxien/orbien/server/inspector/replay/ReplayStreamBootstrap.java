package io.github.lxien.orbien.server.inspector.replay;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 用 EmbeddedChannel 模拟访客，注入重建请求并走既有流状态机
 */
@Component
public class ReplayStreamBootstrap {
    public static final String REPLAY_CLIENT_IP = "127.0.0.1";

    private final StreamManager streamManager;
    private final StateMachine<StreamState, StreamEvent, StreamContext> streamStateMachine;

    @Autowired
    public ReplayStreamBootstrap(StreamManager streamManager,
                                 @Qualifier("streamStateMachine")
                                 StateMachine<StreamState, StreamEvent, StreamContext> streamStateMachine) {
        this.streamManager = streamManager;
        this.streamStateMachine = streamStateMachine;
    }

    public StreamContext start(HttpCaptureRecord source,
                               ByteBuf requestBuf,
                               boolean captureToBuffer,
                               CompletableFuture<HttpCaptureRecord> completion) {
        EmbeddedChannel visitor = new EmbeddedChannel(new ReleaseOutboundHandler());
        try {
            String host = StringUtils.hasText(source.getHost()) ? source.getHost() : "localhost";
            visitor.attr(AttributeKeys.VISIT_DOMAIN).set(stripPort(host));
            // 所有权交给 attr / 后续 relayHttpFirstPackage
            visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).set(requestBuf);

            StreamContext context = streamManager.createStreamContext(visitor, streamStateMachine);
            context.setStreamManager(streamManager);
            context.setProtocol(resolveProtocol(source.getScheme()));
            context.setSourceAddress(REPLAY_CLIENT_IP);
            context.setReplay(true);
            context.setReplaySourceRecordId(source.getId());
            context.setReplayProxyId(source.getProxyId());
            context.setReplayCaptureToBuffer(captureToBuffer);
            context.setReplayCompletion(completion);

            context.fireEvent(StreamEvent.STREAM_OPEN);
            return context;
        } catch (RuntimeException ex) {
            ByteBuf leftover = visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).getAndSet(null);
            safeRelease(leftover);
            finishVisitor(visitor);
            throw ex;
        }
    }

    public void finish(StreamContext context) {
        if (context == null) {
            return;
        }
        Channel visitor = context.getVisitor();
        if (visitor instanceof EmbeddedChannel embedded) {
            finishVisitor(embedded);
        }
    }

    private static void finishVisitor(EmbeddedChannel visitor) {
        if (visitor == null) {
            return;
        }
        try {
            visitor.finishAndReleaseAll();
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
        if (visitor.isActive()) {
            visitor.close();
        }
    }

    private static ProtocolType resolveProtocol(String scheme) {
        if (scheme != null && scheme.equalsIgnoreCase("https")) {
            return ProtocolType.HTTPS;
        }
        return ProtocolType.HTTP;
    }

    private static String stripPort(String host) {
        if (host == null) {
            return null;
        }
        int idx = host.lastIndexOf(':');
        if (idx > 0 && host.indexOf(']') < 0) {
            return host.substring(0, idx);
        }
        return host;
    }

    private static void safeRelease(ByteBuf buf) {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }

    /**
     * 响应写回合成访客时直接释放，避免 EmbeddedChannel 堆积 outbound
     */
    private static final class ReleaseOutboundHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            ReferenceCountUtil.release(msg);
            promise.setSuccess();
        }
    }
}
