package io.github.lxien.orbien.core.message.support;

import java.util.*;

/**
 * 批量消息支持
 */
public final class MultiMessage implements Iterable {
    private final List messages = new ArrayList();

    private MultiMessage() {
    }

    public static MultiMessage createFromCollection(Collection collection) {
        MultiMessage result = new MultiMessage();
        result.addMessages(collection);
        return result;
    }

    public static MultiMessage createFromArray(Object... args) {
        return createFromCollection(Arrays.asList(args));
    }

    public static MultiMessage create() {
        return new MultiMessage();
    }

    public void addMessage(Object msg) {
        messages.add(msg);
    }

    public void addMessages(Collection collection) {
        messages.addAll(collection);
    }

    public Collection getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    public int size() {
        return messages.size();
    }

    public Object get(int index) {
        return messages.get(index);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public Collection removeMessages() {
        Collection result = Collections.unmodifiableCollection(messages);
        messages.clear();
        return result;
    }

    @Override
    public Iterator iterator() {
        return messages.iterator();
    }
}
