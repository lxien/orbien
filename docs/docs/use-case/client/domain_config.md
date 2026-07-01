---
sidebar_position: 5
---

# 域名配置

HTTP 协议代理支持三种域名配置方式：自定义域名、子域名、自动分配域名。

## 自定义域名

配置完整域名，访问时直接使用该域名。

```toml
# etpc.toml
[[proxies]]
name = "web"
protocol = "http"
custom_domains = ["a.domain.com", "b.domain.com"]
targets = [
    { host = "127.0.0.1", port = 8001 }
]
```

## 子域名

配合服务端 `root_domain` 配置使用，只需指定子域名部分，完整域名为 `子域名 + root_domain`。

服务端需配置根域名（详见 [服务端基础配置](../server/base_config.md)）：

```toml
# etps.toml（服务端配置）
root_domain = "domain.com"
```

客户端配置子域名：

```toml
# etpc.toml
[[proxies]]
name = "web"
protocol = "http"
sub_domains = ["a", "b"]
targets = [
    { host = "127.0.0.1", port = 8001 }
]
```

生成的完整域名格式：

- `a.domain.com`
- `b.domain.com`

## 自动分配域名

系统随机生成子域名。

```toml
# etpc.toml
[[proxies]]
name = "web"
protocol = "http"
auto_domain = true
targets = [
    { host = "127.0.0.1", port = 8001 }
]
```

## 参数说明

| 参数名            | 类型              | 默认值  | 描述                         | 必填 |
|:---------------|:----------------|:-----|:---------------------------|:---|
| custom_domains | Array\<String\> | -    | 自定义域名列表，配置完整域名             | 否  |
| sub_domains    | Array\<String\> | -    | 子域名列表，配合服务端 root_domain 使用 | 否  |
| auto_domain    | Boolean         | true | 是否自动分配子域名                  | 否  |

:::tip
三种域名配置方式可同时配置，系统会按优先级依次检查：custom_domains > sub_domains > auto_domain，以优先级最高的方式生成域名。
:::