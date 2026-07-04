package io.github.lxien.orbien.server.web.manager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * 验证码管理 - 使用 Caffeine 缓存
 *
 * @author liuxin
 */
@Component
public class CaptchaManager {
    private final Logger logger = LoggerFactory.getLogger(CaptchaManager.class);
    private final Cache<String, CaptchaEntry> cache;
    public CaptchaManager() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000) 
                .expireAfterWrite(10, TimeUnit.MINUTES) 
                .recordStats() 
                .removalListener((key, value, cause) ->
                        logger.debug("验证码自动移除: key={}, cause={}", key, cause))
                .build();
    }
    /**
     * 存放验证码，返回一个唯一的 captchaId
     */
    public String add(String code, int expireSeconds) {
        String captchaId = UUID.randomUUID().toString().replaceAll("-", "");
        long expireAt = System.currentTimeMillis() + expireSeconds * 1000L;
        cache.put(captchaId, new CaptchaEntry(code.toUpperCase(), expireAt));
        logger.debug("验证码已添加: ID:{}, 过期时间: {}, 当前缓存大小: {}", captchaId, expireAt, cache.estimatedSize());
        return captchaId;
    }
    /**
     * 校验并删除
     */
    public void verifyAndRemove(String captchaId, String code) {
        if (captchaId == null || code == null) {
            throw new BizException("输入验证码为空");
        }
        CaptchaEntry entry = cache.getIfPresent(captchaId);
        try {
            verifyEntry(entry, captchaId, code);
        } finally {
            cache.invalidate(captchaId);
            logger.debug("验证码已删除: {}", captchaId);
        }
    }
    /**
     * 只校验不删除，允许重复校验
     * 注意：这个方法可能被用于暴力尝试，建议谨慎使用
     */
    public void verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            throw new BizException("输入验证码为空");
        }
        CaptchaEntry entry = cache.getIfPresent(captchaId);
        verifyEntry(entry, captchaId, code);
    }
    /**
     * 验证验证码条目
     */
    private void verifyEntry(CaptchaEntry entry, String captchaId, String code) {
        if (entry == null) {
            throw new BizException("验证码不存在或已过期");
        }
        if (entry.expireAt() < System.currentTimeMillis()) {
            logger.debug("验证码已过期: {}", captchaId);
            cache.invalidate(captchaId);
            throw new BizException("验证码已过期");
        }
        boolean result = entry.code().equalsIgnoreCase(code.trim());
        logger.debug("验证码校验结果: {}, 验证码ID: {}", result, captchaId);
        if (!result) {
            throw new BizException("验证码不正确");
        }
    }
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return cache.stats();
    }
    /**
     * 清理所有验证码（管理功能）
     */
    public void clearAll() {
        cache.invalidateAll();
        logger.info("所有验证码已清空");
    }
    /**
     * 验证码条目记录
     */
    public record CaptchaEntry(String code, long expireAt) {
    }
}