
package io.github.lxien.orbien.core.transport;


import java.util.LinkedList;
import java.util.Queue;

import io.github.lxien.orbien.core.message.support.MultiMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

/**
 * 批量写队列
 */
public class NettyBatchWriteQueue extends BatchExecutorQueue<NettyBatchWriteQueue.MessageTuple> {

    private final Channel channel;

    private final EventLoop eventLoop;

    private final Queue<ChannelPromise> promises = new LinkedList<>();

    private final MultiMessage multiMessage = MultiMessage.create();

    private NettyBatchWriteQueue(Channel channel) {
        this.channel = channel;
        this.eventLoop = channel.eventLoop();
    }

    public ChannelFuture enqueue(Object message) {
        return enqueue(message, channel.newPromise());
    }

    public ChannelFuture enqueue(Object message, ChannelPromise channelPromise) {
        MessageTuple messageTuple = new MessageTuple(message, channelPromise);
        super.enqueue(messageTuple, eventLoop);
        return messageTuple.channelPromise;
    }

    @Override
    protected void prepare(MessageTuple item) {
        multiMessage.addMessage(item.originMessage);
        promises.add(item.channelPromise);
    }

    @Override
    protected void flush(MessageTuple item) {
        prepare(item);
        Object finalMessage = multiMessage;
        if (multiMessage.size() == 1) {
            finalMessage = multiMessage.get(0);
        }
        channel.writeAndFlush(finalMessage).addListener((ChannelFutureListener) future -> {
            ChannelPromise cp;
            while ((cp = promises.poll()) != null) {
                if (future.isSuccess()) {
                    cp.setSuccess();
                } else {
                    cp.setFailure(future.cause());
                }
            }
        });
        this.multiMessage.removeMessages();
    }

    public static NettyBatchWriteQueue createWriteQueue(Channel channel) {
        return new NettyBatchWriteQueue(channel);
    }

    static class MessageTuple {

        private final Object originMessage;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object originMessage, ChannelPromise channelPromise) {
            this.originMessage = originMessage;
            this.channelPromise = channelPromise;
        }
    }
}
