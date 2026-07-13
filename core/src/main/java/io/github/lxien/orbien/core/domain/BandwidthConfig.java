package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.core.utils.BandwidthParser;
import lombok.Getter;

@Getter
public class BandwidthConfig {
    /**
     * 总带宽（出站+入站）
     */
    private String limitTotal;
    /**
     * 入站带宽
     */
    private String limitIn;
    /**
     * 出站带宽
     */
    private String limitOut;

    private final Long totalBps;
    private final Long inBps;
    private final Long outBps;

    public BandwidthConfig(String limitTotal, String limitIn, String limitOut) {
        this.limitTotal = limitTotal;
        this.limitIn = limitIn;
        this.limitOut = limitOut;
        this.totalBps = BandwidthParser.parseToBps(limitTotal);
        this.inBps = BandwidthParser.parseToBps(limitIn);
        this.outBps = BandwidthParser.parseToBps(limitOut);
        validate();
    }

    public BandwidthConfig(Long totalBps, Long inBps, Long outBps) {
        this.totalBps = totalBps;
        this.inBps = inBps;
        this.outBps = outBps;
        validate();
    }

    private void validate() {
        if (totalBps != null && totalBps <= 0) {
            throw new IllegalArgumentException("总带宽限制必须大于 0");
        }

        if (inBps != null && inBps < 0) {
            throw new IllegalArgumentException("入站带宽限制必须大于等于 0");
        }

        if (outBps != null && outBps < 0) {
            throw new IllegalArgumentException("出站带宽限制必须大于等于 0");
        }
        // 总量约束
        if (totalBps != null) {

            if (inBps != null && inBps > totalBps) {
                throw new IllegalArgumentException("入站带宽限制不能大于总带宽限制");
            }

            if (outBps != null && outBps > totalBps) {
                throw new IllegalArgumentException("出站带宽限制不能大于总带宽限制");
            }

            if (inBps != null && outBps != null && inBps + outBps > totalBps) {
                throw new IllegalArgumentException("入站带宽限制与出站带宽限制之和不能大于总带宽限制");
            }
        }
    }

    public boolean hasLimitConfigured() {
        return totalBps != null || inBps != null || outBps != null;
    }

    public boolean hasLimitInConfigured() {
        return inBps != null && inBps > 0;
    }

    public boolean hasLimitOutConfigured() {
        return outBps != null && outBps > 0;
    }

    public boolean hasLimitTotalConfigured() {
        return totalBps != null && totalBps > 0;
    }
}
