package io.github.lxien.orbien.server.web.dto.proxy;

import io.github.lxien.orbien.server.web.dto.loadbalance.LoadBalanceDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TcpProxyDetailDTO {
    private String id;
    private String agentId;
    private String name;
    /**
     * 用户指定的远程端口，自动分配时为 null
     */
    private Integer remotePort;
    /**
     * 实际监听端口
     */
    private Integer listenPort;
    private String localHost;
    private Integer localPort;
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
