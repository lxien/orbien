---
sidebar_position: 10
---

# 获取真实来源IP

当代理服务转发请求时，后端服务默认只能看到**代理服务器**的IP地址。所以代理服务（`orbiens`）会将真实的访问来源IP地址通过**X-Forwarded-For**头进行传递，仅支持**HTTP**协议。

### 请求头格式

```http
X-Forwarded-For: <visitor-ip>, <proxy-1-ip>, <proxy-2-ip>
```

:::tip
访问请求可能会经过多级代理/负载均衡，所以按照惯例，第一个IP地址是最初的访问来源IP地址，后续的IP地址是经过的代理服务器的IP地址。
:::

### 使用示例

内网真实后端服务可以通过读取 `X-Forwarded-For` 请求头获取真实访问来源IP：

```java
String visitorIp = request.getHeader("X-Forwarded-For");
```