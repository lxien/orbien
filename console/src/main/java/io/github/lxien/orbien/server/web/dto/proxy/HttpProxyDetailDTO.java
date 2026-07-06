package io.github.lxien.orbien.server.web.dto.proxy;

import io.github.lxien.orbien.server.web.dto.loadbalance.LoadBalanceDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HttpProxyDetailDTO {
    private String id;
    private String agentId;
    private String name;
    private Integer domainType;
    /**
     * 完整自定义域名，仅 domainType=CUSTOM_DOMAIN 时有值
     */
    private List<String> customDomains;
    /**
     * 子域名绑定，仅 domainType=SUBDOMAIN 时有值
     */
    private List<SubdomainBindingDTO> subdomainBindings;
    private String localHost;
    private Integer localPort;
    /**
     * 全部内网后端（用于判断是否负载均衡模式）
     */
    private List<TargetDTO> targets;
    private LoadBalanceDTO loadBalance;
    /**
     * 总带宽 Mbps
     */
    private Integer limitTotal;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
