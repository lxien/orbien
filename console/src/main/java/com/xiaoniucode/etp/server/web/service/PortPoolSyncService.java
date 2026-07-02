package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.core.domain.PortInterval;
import com.xiaoniucode.etp.core.enums.PortPoolType;
import com.xiaoniucode.etp.core.utils.PortIntervalUtils;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.config.domain.PortPoolConfig;
import com.xiaoniucode.etp.server.exceptions.EtpException;
import com.xiaoniucode.etp.server.port.PortPoolManager;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.entity.PortPoolDO;
import com.xiaoniucode.etp.server.web.repository.PortPoolRepository;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 端口池同步：TOML 首次落库、DB 加载到内存、Console CRUD 增量同步。
 */
@Service
public class PortPoolSyncService {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PortPoolSyncService.class);

    @Resource
    private PortPoolRepository portPoolRepository;
    @Resource
    private PortPoolManager portPoolManager;
    @Resource
    private AppConfig appConfig;

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        importTomlIfAbsent();
        reloadFromDb();
        logger.info("端口池已从数据库加载到内存");
    }

    /** TOML 区间落库，同协议+同范围已存在则跳过 */
    private void importTomlIfAbsent() {
        PortPoolConfig config = appConfig.getPortPool();
        importIntervals(PortPoolType.TCP, config.getTcp());
        importIntervals(PortPoolType.UDP, config.getUdp());
    }

    private void importIntervals(PortPoolType type, List<PortInterval> intervals) {
        for (PortInterval interval : intervals) {
            int startPort = interval.start();
            Integer endPort = interval.isSinglePort() ? null : interval.end();
            if (portPoolRepository.existsByTypeAndStartPortAndEndPort(type, startPort, endPort)) {
                continue;
            }
            PortPoolDO entity = new PortPoolDO();
            entity.setType(type);
            entity.setStartPort(startPort);
            entity.setEndPort(endPort);
            entity.setRemark("from toml");
            portPoolRepository.save(entity);
            logger.debug("TOML 端口池落库: {} {}-{}", type, startPort, endPort == null ? startPort : endPort);
        }
    }

    public void reloadFromDb() {
        for (PortPoolType type : PortPoolType.values()) {
            List<PortInterval> intervals = portPoolRepository.findByType(type).stream()
                    .map(this::toInterval)
                    .toList();
            portPoolManager.replaceAllowed(type, intervals);
        }
    }

    public void onCreated(PortPoolDO entity) {
        portPoolManager.addAllowed(entity.getType(), toInterval(entity));
    }

    public void onDeleted(PortPoolDO entity) {
        portPoolManager.removeAllowed(entity.getType(), toInterval(entity));
    }

    public void onUpdated(PortPoolDO before, PortPoolDO after) {
        PortPoolType beforeType = before.getType();
        PortPoolType afterType = after.getType();
        PortInterval oldInterval = toInterval(before);
        PortInterval newInterval = toInterval(after);

        if (beforeType != afterType) {
            portPoolManager.removeAllowed(beforeType, oldInterval);
            portPoolManager.addAllowed(afterType, newInterval);
            return;
        }

        List<PortInterval> removed = PortIntervalUtils.subtractAll(List.of(oldInterval), List.of(newInterval));
        List<PortInterval> added = PortIntervalUtils.subtractAll(List.of(newInterval), List.of(oldInterval));
        for (PortInterval interval : removed) {
            portPoolManager.removeAllowed(afterType, interval);
        }
        for (PortInterval interval : added) {
            portPoolManager.addAllowed(afterType, interval);
        }
    }

    public void validateUpdate(PortPoolDO before, PortPoolDO after) {
        try {
            doValidateUpdate(before, after);
        } catch (EtpException e) {
            throw new BizException(e.getMessage());
        }
    }

    public void validateRemovable(PortPoolDO entity) {
        try {
            portPoolManager.validateRemovable(entity.getType(), toInterval(entity));
        } catch (EtpException e) {
            throw new BizException(e.getMessage());
        }
    }

    private void doValidateUpdate(PortPoolDO before, PortPoolDO after) {
        PortInterval oldInterval = toInterval(before);
        PortInterval newInterval = toInterval(after);
        if (before.getType() != after.getType()) {
            portPoolManager.validateRemovable(before.getType(), oldInterval);
            return;
        }
        List<PortInterval> removed = PortIntervalUtils.subtractAll(List.of(oldInterval), List.of(newInterval));
        for (PortInterval interval : removed) {
            portPoolManager.validateRemovable(after.getType(), interval);
        }
    }

    private PortInterval toInterval(PortPoolDO entity) {
        return PortInterval.ofRange(entity.getStartPort(), entity.getEndPort());
    }
}
