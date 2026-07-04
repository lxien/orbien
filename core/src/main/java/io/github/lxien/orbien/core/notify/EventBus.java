package io.github.lxien.orbien.core.notify;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 基于Disruptor 实现的事件总线
 */
public class EventBus {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(EventBus.class);

    private static final int DEFAULT_RING_BUFFER_SIZE = 16384;
    /**
     * 事件-->监听器列表
     */
    private final Map<Class<?>, List<EventListener<?>>> subscribers = new ConcurrentHashMap<>();

    private final Disruptor<GenericEvent> disruptor;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public EventBus() {
        this(DEFAULT_RING_BUFFER_SIZE);
    }

    public EventBus(int ringBufferSize) {
        this(ringBufferSize, new BlockingWaitStrategy());
    }

    public EventBus(int ringBufferSize, WaitStrategy waitStrategy) {
        ThreadFactory threadFactory = new SimpleNamedThreadFactory("DisruptorEventBus");

        this.disruptor = new Disruptor<>(
                GenericEvent::new,
                ringBufferSize,
                threadFactory,
                ProducerType.MULTI,
                waitStrategy
        );

        this.disruptor.handleEventsWith(new AsyncBatchDispatcher());
        this.disruptor.start();
    }

    /**
     * 注册监听器
     */
    public <T> void register(EventListener<T> listener, Class<T> eventType) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * 注册监听器
     */
    public <T> void register(EventListener<T> listener) {
        Class<?> eventType = getEventType(listener);
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public void unregister(EventListener<?> listener) {
        for (List<EventListener<?>> list : subscribers.values()) {
            list.remove(listener);
        }
    }

    /**
     * 同步发布：在当前线程直接调用，只传递 event
     */
    @SuppressWarnings("unchecked")
    public void publishSync(Object event) {
        checkRunning();
        if (event == null) return;

        // 设置同步事件的 sequence 和 endOfBatch
        if (event instanceof Event) {
            ((Event) event).setSequence(-1L);
            ((Event) event).setEndOfBatch(false);
        }

        List<EventListener<?>> listeners = getAllListeners(event.getClass());
        for (EventListener<?> listener : listeners) {
            try {
                // 同步调用只传 event
                ((EventListener<Object>) listener).onEvent(event);
            } catch (Exception e) {
                handleException(listener, event, e);
            }
        }
    }

    /**
     * 异步发布：进入RingBuffer
     */
    public void publishAsync(Object event) {
        checkRunning();
        if (event == null) return;

        disruptor.getRingBuffer().publishEvent(
                (holder, seq, e) -> holder.set(e),
                event
        );
    }

    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            disruptor.shutdown();
        }
    }

    private void checkRunning() {
        if (!running.get()) {
            throw new IllegalStateException("EventBus 已关闭");
        }
    }

    /**
     * 获取指定类型及其父类型的所有监听器
     */
    private List<EventListener<?>> getAllListeners(Class<?> eventType) {
        List<EventListener<?>> listeners = new CopyOnWriteArrayList<>();
        Class<?> currentType = eventType;
        while (currentType != null) {
            List<EventListener<?>> typeListeners = subscribers.get(currentType);
            if (typeListeners != null) {
                listeners.addAll(typeListeners);
            }
            currentType = currentType.getSuperclass();
        }
        // 还需要考虑接口类型
        for (Class<?> interfaceType : eventType.getInterfaces()) {
            List<EventListener<?>> interfaceListeners = subscribers.get(interfaceType);
            if (interfaceListeners != null) {
                listeners.addAll(interfaceListeners);
            }
        }
        return listeners;
    }

    /**
     * 从监听器中解析出事件类型
     */
    private Class<?> getEventType(EventListener<?> listener) {
        // 尝试从类层次结构中获取泛型类型
        Type type = listener.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                return (Class<?>) typeArguments[0];
            }
        }
        // 尝试从接口中获取泛型类型
        Type[] interfaces = listener.getClass().getGenericInterfaces();
        for (Type interfaceType : interfaces) {
            if (interfaceType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                if (parameterizedType.getRawType() == EventListener.class) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }
        return Object.class;
    }

    private static class GenericEvent {
        private Object value;

        void set(Object value) {
            this.value = value;
        }

        Object get() {
            return value;
        }

        void clear() {
            value = null;
        }
    }

    private class AsyncBatchDispatcher implements EventHandler<GenericEvent> {
        @SuppressWarnings("unchecked")
        @Override
        public void onEvent(GenericEvent holder, long sequence, boolean endOfBatch) {
            Object event = holder.get();
            if (event == null) return;

            // 设置异步事件的 sequence 和 endOfBatch
            if (event instanceof Event) {
                ((Event) event).setSequence(sequence);
                ((Event) event).setEndOfBatch(endOfBatch);
            }

            List<EventListener<?>> listeners = getAllListeners(event.getClass());

            for (EventListener<?> listener : listeners) {
                try {
                    ((EventListener<Object>) listener).onEvent(event);
                } catch (Exception e) {
                    handleException(listener, event, e);
                }
            }

            holder.clear();
        }
    }

    private void handleException(EventListener<?> listener, Object event, Exception e) {
        logger.error("事件异常", e);
    }


    private static class SimpleNamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        public SimpleNamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
