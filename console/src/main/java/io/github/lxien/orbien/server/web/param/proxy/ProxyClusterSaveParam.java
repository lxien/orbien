package io.github.lxien.orbien.server.web.param.proxy;

import io.github.lxien.orbien.server.web.param.loadbalance.LoadBalanceParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProxyClusterSaveParam {
    @NotNull(message = "负载均衡策略不能为空")
    @Valid
    private LoadBalanceParam loadBalance;

    @NotEmpty(message = "请至少添加一个服务")
    @Valid
    private List<ProxyTargetSaveParam> targets;
}
