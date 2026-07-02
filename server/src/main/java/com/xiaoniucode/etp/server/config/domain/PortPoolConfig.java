package com.xiaoniucode.etp.server.config.domain;

import com.xiaoniucode.etp.core.domain.PortInterval;
import com.xiaoniucode.etp.core.utils.PortIntervalUtils;
import lombok.Getter;

import java.util.List;

@Getter
public class PortPoolConfig {
    private final List<PortInterval> tcp;
    private final List<PortInterval> udp;

    public PortPoolConfig(List<PortInterval> tcp, List<PortInterval> udp) {
        this.tcp = tcp == null ? List.of() : List.copyOf(tcp);
        this.udp = udp == null ? List.of() : List.copyOf(udp);
    }

    public static PortPoolConfig empty() {
        return new PortPoolConfig(List.of(), List.of());
    }

    public List<PortInterval> mergedTcp() {
        return PortIntervalUtils.merge(tcp);
    }

    public List<PortInterval> mergedUdp() {
        return PortIntervalUtils.merge(udp);
    }
}
