package io.github.lxien.orbien.server.statemachine.agent.action.config;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.github.lxien.orbien.server.exceptions.OrbienException;
import io.github.lxien.orbien.server.exceptions.PortConflictException;
import io.github.lxien.orbien.server.port.PortPoolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListenPortResolver {

    @Autowired
    private PortPoolManager portPoolManager;

    public Integer resolve(int remotePort, ProxyConfigExt ext, PortPoolType poolType) {
        if (remotePort >= 1) {
            if (!portPoolManager.isAvailable(poolType, remotePort)) {
                throw new PortConflictException(remotePort);
            }
            portPoolManager.reserve(poolType, remotePort);
            return remotePort;
        }
        if (ext != null && ext.getProxyConfig().getListenPort() != null) {
            Integer existingPort = ext.getProxyConfig().getListenPort();
            if (existingPort > 0 && portPoolManager.isAvailable(poolType, existingPort)) {
                portPoolManager.reserve(poolType, existingPort);
                return existingPort;
            }
        }
        Integer allocated = portPoolManager.acquire(poolType);
        if (allocated == null) {
            throw new OrbienException("没有可用的端口");
        }
        return allocated;
    }
}
