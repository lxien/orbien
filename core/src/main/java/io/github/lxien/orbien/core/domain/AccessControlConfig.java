package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.core.enums.AccessControl;
import lombok.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class AccessControlConfig implements Serializable {
    @Setter
    private boolean enabled;
    @Setter
    private AccessControl mode;
    private final Set<String> allow = new CopyOnWriteArraySet<>();
    private final Set<String> deny = new CopyOnWriteArraySet<>();

    public AccessControlConfig(Boolean enabled, AccessControl mode,
                               Set<String> allow, Set<String> deny) {
        this.enabled = enabled != null && enabled;
        this.mode = mode;

        if (allow != null && !allow.isEmpty()) {
            this.allow.addAll(allow);
        }
        if (deny != null && !deny.isEmpty()) {
            this.deny.addAll(deny);
        }
    }

    public boolean hasAllow() {
        return !allow.isEmpty();
    }

    public boolean hasDeny() {
        return !deny.isEmpty();
    }

    /**
     * 添加 allow 规则
     */
    public void addAllow(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return;
        }
        allow.add(cidr);
    }

    /**
     * 添加 deny 规则
     */
    public void addDeny(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return;
        }
        deny.add(cidr);
    }

    /**
     * 删除 allow 规则
     */
    public void removeAllow(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return;
        }
        allow.remove(cidr);
    }

    /**
     * 删除 deny 规则
     */
    public void removeDeny(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return;
        }
        deny.remove(cidr);
    }

    /**
     * 清空所有规则
     */
    public void clear() {
        allow.clear();
        deny.clear();
    }

    /**
     * 只读视图
     */
    public Set<String> getAllowView() {
        return Collections.unmodifiableSet(allow);
    }

    public Set<String> getDenyView() {
        return Collections.unmodifiableSet(deny);
    }
}