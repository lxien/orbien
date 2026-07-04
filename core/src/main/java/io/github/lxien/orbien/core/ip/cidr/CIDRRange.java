package io.github.lxien.orbien.core.ip.cidr;

/**
 * CIDR IP区间
 */
public class CIDRRange {

    private final long startIp;
    private final long endIp;

    public CIDRRange(long startIp, long endIp) {
        this.startIp = startIp;
        this.endIp = endIp;
    }

    public long getStartIp() {
        return startIp;
    }

    public long getEndIp() {
        return endIp;
    }

    /**
     * 检查IP是否在区间内
     */
    public boolean contains(long ip) {
        return ip >= startIp && ip <= endIp;
    }
}
