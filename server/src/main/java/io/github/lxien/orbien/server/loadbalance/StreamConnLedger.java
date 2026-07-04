package io.github.lxien.orbien.server.loadbalance;

import java.util.concurrent.ConcurrentHashMap;

public final class StreamConnLedger {
    private final ConcurrentHashMap<Integer, String> streamKey = new ConcurrentHashMap<>();

    /**
     * 打开成功后 bind；若重复 bind（重入）会返回 false
     */
    public boolean bindIfAbsent(int streamId, String key) {
        return streamKey.putIfAbsent(streamId, key) == null;
    }

    /**
     * close 时 unbind；返回 null 表示已经释放过（幂等）
     */
    public String unbind(int streamId) {
        return streamKey.remove(streamId);
    }

    /**
     * 失败但已选择目标的场景，也可以用它判断是否需要释放
     */
    public boolean isBound(int streamId) {
        return streamKey.containsKey(streamId);
    }
}