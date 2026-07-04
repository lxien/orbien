package io.github.lxien.orbien.server.configuration;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.cola.statemachine.builder.StateMachineBuilder;
import com.alibaba.cola.statemachine.builder.StateMachineBuilderFactory;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.action.*;
import io.github.lxien.orbien.server.statemachine.stream.action.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 服务端流状态机配置
 * 负责配置服务端流的状态机，管理流的各种状态转换
 */
@Component
public class StreamStateMachineConfig {
    /**
     * 目标解析动作
     */
    @Autowired
    private TargetResolverAction targetResolverAction;

    /**
     * 流打开动作
     */
    @Autowired
    private StreamOpenAction streamOpenAction;

    /**
     * 流打开响应动作
     */
    @Autowired
    private StreamOpenResponseAction streamOpenResponseAction;

    /**
     * 流关闭动作
     */
    @Autowired
    private StreamCloseAction streamCloseAction;

    @Autowired
    private StreamPauseAction streamPauseAction;

    @Autowired
    private StreamResumeAction streamResumeAction;

    /**
     * 创建流状态机
     *
     * @return 流状态机实例
     */
    @Bean("streamStateMachine")
    public StateMachine<StreamState, StreamEvent, StreamContext> create() {
        StateMachineBuilder<StreamState, StreamEvent, StreamContext> builder = StateMachineBuilderFactory.create();

        // 打开流
        builder.externalTransition()
                .from(StreamState.IDLE)
                .to(StreamState.OPENING)
                .on(StreamEvent.STREAM_OPEN)
                .when(ctx -> true)
                .perform(targetResolverAction);

        // 目标验证完成
        builder.internalTransition()
                .within(StreamState.OPENING)
                .on(StreamEvent.TARGET_VALIDATED)
                .perform(streamOpenAction);

        // 打开流成功
        builder.externalTransition()
                .from(StreamState.OPENING)
                .to(StreamState.OPENED)
                .on(StreamEvent.STREAM_OPEN_SUCCESS)
                .perform(streamOpenResponseAction);
        // 暂停流
        builder.externalTransition()
                .from(StreamState.OPENED)
                .to(StreamState.PAUSED)
                .on(StreamEvent.STREAM_LOCAL_PAUSE)
                .perform(streamPauseAction);
        builder.externalTransition()
                .from(StreamState.OPENED)
                .to(StreamState.PAUSED)
                .on(StreamEvent.STREAM_REMOTE_PAUSE)
                .perform(streamPauseAction);
        // 恢复流
        builder.externalTransition()
                .from(StreamState.PAUSED)
                .to(StreamState.OPENED)
                .on(StreamEvent.STREAM_LOCAL_RESUME)
                .perform(streamResumeAction);
        builder.externalTransition()
                .from(StreamState.PAUSED)
                .to(StreamState.OPENED)
                .on(StreamEvent.STREAM_REMOTE_RESUME)
                .perform(streamResumeAction);
        // 打开流失败
        builder.externalTransition()
                .from(StreamState.OPENING)
                .to(StreamState.FAILED)
                .on(StreamEvent.STREAM_OPEN_FAILURE)
                .when(ctx -> true)
                .perform(streamCloseAction);

        // 本地关闭流事件
        builder.externalTransitions()
                .fromAmong(StreamState.OPENED,StreamState.FAILED, StreamState.OPENING, StreamState.PAUSED)
                .to(StreamState.CLOSED)
                .on(StreamEvent.STREAM_LOCAL_CLOSE)
                .when(ctx -> true)
                .perform(streamCloseAction);

        // 来自远程的关闭流事件
        builder.externalTransitions()
                .fromAmong(StreamState.OPENED, StreamState.FAILED, StreamState.OPENING, StreamState.PAUSED)
                .to(StreamState.CLOSED)
                .on(StreamEvent.STREAM_REMOTE_CLOSE)
                .when(ctx -> true)
                .perform(streamCloseAction);
        return builder.build("stream-state-machine");
    }
}
