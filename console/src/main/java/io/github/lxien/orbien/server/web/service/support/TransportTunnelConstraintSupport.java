package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.server.web.dto.transport.TransportTunnelConstraints;

import java.util.List;

/**
 * 隧道模式字段的可编辑性约束
 */
public final class TransportTunnelConstraintSupport {

    private TransportTunnelConstraintSupport() {
    }

    public static TransportTunnelConstraints build(ProtocolType proxyProtocol, TransportProtocol dataProtocol, Boolean storedMultiplex) {
        TransportTunnelConstraints constraints = new TransportTunnelConstraints();
        boolean multiplexOnly = requiresMultiplexOnly(proxyProtocol, dataProtocol);

        if (multiplexOnly) {
            constraints.setTunnelEditable(false);
            constraints.setTunnelLocked(true);
            constraints.setTunnelLockedReason(lockReason(proxyProtocol, dataProtocol));
            constraints.setAllowedTunnelTypes(List.of(TunnelType.MULTIPLEX.getCode()));
            return constraints;
        }

        constraints.setTunnelEditable(true);
        constraints.setTunnelLocked(false);
        constraints.setAllowedTunnelTypes(List.of(TunnelType.MULTIPLEX.getCode(), TunnelType.DIRECT.getCode()));
        return constraints;
    }

    public static boolean requiresMultiplexOnly(ProtocolType proxyProtocol, TransportProtocol dataProtocol) {
        if (proxyProtocol != null && proxyProtocol.isUdp()) {
            return true;
        }
        return dataProtocol != null && !dataProtocol.isSupportsDirect();
    }

    public static boolean resolveEffectiveMultiplex(ProtocolType proxyProtocol, TransportProtocol dataProtocol, Boolean storedMultiplex) {
        if (proxyProtocol != null && proxyProtocol.isUdp()) {
            return true;
        }
        boolean multiplex = storedMultiplex == null || storedMultiplex;
        return TransportEndpointResolver.normalizeMultiplex(dataProtocol, multiplex);
    }

    public static int resolveEffectiveTunnelType(ProtocolType proxyProtocol, TransportProtocol dataProtocol, Boolean storedMultiplex) {
        return resolveEffectiveMultiplex(proxyProtocol, dataProtocol, storedMultiplex)
                ? TunnelType.MULTIPLEX.getCode()
                : TunnelType.DIRECT.getCode();
    }

    public static int resolveStoredTunnelType(Boolean storedMultiplex) {
        return Boolean.FALSE.equals(storedMultiplex)
                ? TunnelType.DIRECT.getCode()
                : TunnelType.MULTIPLEX.getCode();
    }

    private static String lockReason(ProtocolType proxyProtocol, TransportProtocol dataProtocol) {
        if (proxyProtocol != null && proxyProtocol.isUdp()) {
            return "udp_requires_multiplex";
        }
        if (dataProtocol == TransportProtocol.QUIC) {
            return "quic_requires_multiplex";
        }
        return "protocol_requires_multiplex";
    }
}
