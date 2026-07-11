package io.github.lxien.orbien.server.transport.haproxy;

import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.server.ip.cidr.CIDRMatcher;

import java.util.Set;

/**
 * 校验 PROXY 头来源是否为受信任的 LB（防伪造真实 IP）
 */
final class TrustedProxyMatcher {

    private final CIDRMatcher matcher;
    private final boolean trustAll;

    TrustedProxyMatcher(Set<String> trustedProxies) {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            trustAll = true;
            matcher = null;
        } else {
            trustAll = false;
            matcher = new CIDRMatcher(AccessControl.ALLOW, trustedProxies, Set.of());
        }
    }

    boolean isTrustedPeer(String peerIp) {
        if (peerIp == null || peerIp.isBlank()) {
            return false;
        }
        return trustAll || matcher.isAllowed(peerIp);
    }
}
