---
sidebar_position: 1
---
# TLS 加密


## TLS 传输加密

orbien 支持 TLS 传输加密，保证代理服务端与代理客户端端之间安全地进行数据传输，如果边缘客户端JDK版本在11及以上，则使用 TLSv1.3 协议，否则使用 TLSv1.2 协议。

## 配置模式

:::warning
在没有自定义证书配置情况下，系统**默认采用自签名证书**，生产环境请务必使用正式签发的证书。
:::

### 1. 单向验证

**服务端：**

```toml
[transport]
[transport.tls]
enabled = true
cert_file = "cert/server.crt"
key_file = "cert/server.key"
```

客户端无需配置。

### 4. 双向验证 (mTLS)

服务端和客户端互相验证，双方均需配置证书：

**服务端：**

```toml
[transport]
[transport.tls]
enabled = true
cert_file = "cert/server.crt"
key_file = "cert/server.key"
ca_file = "cert/ca.crt"
```

**客户端：**

```toml
[transport]
[transport.tls]
enabled = true
cert_file = "cert/client.crt"
key_file = "cert/client.key"
ca_file = "cert/ca.crt"
```

## 参数说明

| 参数名       | 类型      | 默认值  | 描述                        | 必填    |
|:----------|:--------|:-----|:--------------------------|:------|
| enabled   | Boolean | true | 是否启用 TLS 加密               | 否     |
| cert_file | String  | -    | 证书文件路径（PEM 格式）            | 启用时必填 |
| key_file  | String  | -    | 私钥文件路径（PEM 格式）            | 启用时必填 |
| ca_file   | String  | -    | CA 证书文件路径，配置后启用 mTLS 双向认证 | 否     |
| key_pass  | String  | -    | 私钥密码                      | 否     |

