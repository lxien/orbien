package io.github.lxien.orbien.core.notify;

/**
 * 事件监听器接口
 */
@FunctionalInterface
public interface EventListener<T> {
    /**
     * 事件回调
     *
     * @param event 事件对象
     */
    void onEvent(T event);
}
