---
sidebar_position: 2
---

参考案例：

```toml
[[proxies]]
name = "web"
protocol = "http"
custom_domains = ["a.domain.com", "b.domain.com"]
targets = [
    { host = "127.0.0.1", port = 8001, weight = 1, name = "web-instance-1" }
]
```

如果配置了根域名（[root_domain](../../server/base_config.md)）可以自定义子域名：

```toml
[[proxies]]
name = "web"
protocol = "http"
sub_domains = ["a", "b"]
targets = [
    { host = "127.0.0.1", port = 8001, weight = 1, name = "web-instance-1" }
]
```

或采用自动分配域名，系统会随机生成一个子域名

```toml
[[proxies]]
name = "web"
protocol = "http"
auto_domain = true
targets = [
    { host = "127.0.0.1", port = 8001, weight = 1, name = "web-instance-1" }
]
```

远程访问地址：

- [http://a.domain.com:http_proxy_port](http://a.domain.com)
- [http://b.domain.com:http_proxy_port](http://b.domain.com)

:::tip
http_proxy_port 是服务端配置的 HTTP 代理端口，默认为
80，访问时需要替换为实际配置的端口号。[查看服务端配置文档](../../server/base_config.md)，
更多细节请参考[域名配置](../domain_config.md)
:::

## 参数说明

| 参数名            | 类型                  | 默认值  | 描述                         | 必填 |
|:---------------|:--------------------|:-----|:---------------------------|:---|
| name           | String              | -    | 代理名称，唯一标识一个代理配置            | 是  |
| protocol       | String              | -    | 协议类型                       | 是  |
| custom_domains | Array&lt;String&gt; | -    | 自定义域名列表，配置完整域名             | 否  |
| sub_domains    | Array&lt;String&gt; | -    | 子域名列表，配合服务端 root_domain 使用 | 否  |
| auto_domain    | Boolean             | true | 是否自动分配子域名                  | 否  |
| targets        | Array&lt;Object&gt; | -    | 目标服务列表，内网真实服务列表，支持单机和集群    | 是  |
| enabled        | Boolean             | true | 是否开启代理                     | 否  |

### targets 子参数说明

| 参数名    | 类型      | 默认值       | 描述                | 必填 |
|:-------|:--------|:----------|:------------------|:---|
| host   | String  | 127.0.0.1 | 目标服务主机地址          | 否  |
| port   | Integer | -         | 目标服务端口            | 是  |
| weight | Integer | 1         | 负载均衡权重，用于权重负载均衡算法 | 否  |
| name   | String  | -         | 目标服务名称            | 否  |
