package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.api.TransportEncryptResolver;
import io.github.lxien.orbien.server.web.dto.transport.TransportEncryptConstraints;

import java.util.List;

/**
 * 管理面板：传输加密字段的可编辑性约束。
 */
public final class TransportEncryptConstraintSupport {

    private TransportEncryptConstraintSupport() {
    }

    public static TransportEncryptConstraints build(TransportProtocol dataProtocol, boolean globalTlsEnabled,
                                                    Boolean storedEncrypt) {
        TransportEncryptConstraints constraints = new TransportEncryptConstraints();
        constraints.setGlobalTlsEnabled(globalTlsEnabled);

        if (TransportEncryptResolver.requiresTls(dataProtocol)) {
            constraints.setEncryptEditable(false);
            constraints.setEncryptLocked(true);
            constraints.setEncryptLockedReason("protocol_requires_tls");
            constraints.setAllowedEncryptValues(List.of(true));
            return constraints;
        }

        if (!globalTlsEnabled) {
            constraints.setEncryptEditable(false);
            constraints.setEncryptLocked(false);
            constraints.setEncryptLockedReason("global_tls_disabled");
            constraints.setAllowedEncryptValues(List.of(false));
            return constraints;
        }

        constraints.setEncryptEditable(true);
        constraints.setEncryptLocked(false);
        constraints.setAllowedEncryptValues(List.of(true, false));
        return constraints;
    }

    public static boolean resolveStoredEncrypt(Boolean storedEncrypt) {
        return storedEncrypt == null || storedEncrypt;
    }

    public static boolean resolveEffectiveEncrypt(TransportProtocol dataProtocol,
                                                 boolean globalTlsEnabled,
                                                 Boolean storedEncrypt) {
        return TransportEncryptResolver.resolveEffectiveEncrypt(
                dataProtocol, globalTlsEnabled, storedEncrypt);
    }

    public static boolean toMultiplex(Integer tunnelType) {
        TunnelType type = TunnelType.fromCode(tunnelType);
        if (type == null) {
            throw new IllegalArgumentException("无效的隧道类型");
        }
        return type.isMultiplex();
    }
}
