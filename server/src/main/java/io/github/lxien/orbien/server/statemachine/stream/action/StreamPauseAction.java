/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.statemachine.stream.action;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StreamPauseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamPauseAction.class);
    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        logger.debug("暂停流 {} 读取",context.getStreamId());
        Channel visitor = context.getVisitor();
        if (event == StreamEvent.STREAM_REMOTE_PAUSE && visitor != null) {
            context.pauseVisitorRead(StreamContext.VISITOR_PAUSE_REMOTE);
        }
        if (event == StreamEvent.STREAM_LOCAL_PAUSE) {
            sendPauseToRemote(context);
        }
    }

    private void sendPauseToRemote(StreamContext context) {
        logger.debug("通知边缘客户端暂停流 {} 读取", context.getStreamId());
        TMSPFrame frame = new TMSPFrame(context.getStreamId(), TMSP.MSG_STREAM_PAUSE);
        context.getControl().writeAndFlush(frame);
    }
}
