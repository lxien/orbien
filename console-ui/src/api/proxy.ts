import request from '@/utils/http'

/**
 * 获取 HTTP 代理列表（分页）
 * @param params 分页参数
 * @returns HTTP 代理分页列表
 */
export function fetchGetHttpProxyList(params: Api.Common.CommonSearchParams) {
    return request.get<Api.Common.PaginatedResponse<Api.Proxy.HttpProxyListDTO>>({
        url: '/api/proxies/http',
        params
    })
}

/**
 * 获取 TCP 代理列表（分页）
 * @param params 分页参数
 * @returns TCP 代理分页列表
 */
export function fetchGetTcpProxyList(params: Api.Common.CommonSearchParams) {
    return request.get<Api.Common.PaginatedResponse<Api.Proxy.TcpProxyListDTO>>({
        url: '/api/proxies/tcp',
        params
    })
}

/**
 * 获取 UDP 代理列表（分页）
 */
export function fetchGetUdpProxyList(params: Api.Common.CommonSearchParams) {
    return request.get<Api.Common.PaginatedResponse<Api.Proxy.UdpProxyListDTO>>({
        url: '/api/proxies/udp',
        params
    })
}

/**
 * 获取 HTTP 代理详情
 * @param id 代理 ID
 * @returns HTTP 代理详情
 */
export function fetchGetHttpProxyById(id: string) {
    return request.get<Api.Proxy.HttpProxyDetailDTO>({
        url: `/api/proxies/http/${id}`
    })
}

/**
 * 获取 HTTPS 代理列表（分页）
 * @param params 分页参数
 * @returns HTTPS 代理分页列表
 */
export function fetchGetHttpsProxyList(params: Api.Common.CommonSearchParams) {
    return request.get<Api.Common.PaginatedResponse<Api.Proxy.HttpsProxyListDTO>>({
        url: '/api/proxies/https',
        params
    })
}

/**
 * 获取 HTTPS 代理详情
 * @param id 代理 ID
 * @returns HTTPS 代理详情
 */
export function fetchGetHttpsProxyById(id: string) {
    return request.get<Api.Proxy.HttpsProxyDetailDTO>({
        url: `/api/proxies/https/${id}`
    })
}

/**
 * 获取 TCP 代理详情
 * @param id 代理 ID
 * @returns TCP 代理详情
 */
export function fetchGetTcpProxyById(id: string) {
    return request.get<Api.Proxy.TcpProxyDetailDTO>({
        url: `/api/proxies/tcp/${id}`
    })
}

/**
 * 获取 UDP 代理详情
 */
export function fetchGetUdpProxyById(id: string) {
    return request.get<Api.Proxy.UdpProxyDetailDTO>({
        url: `/api/proxies/udp/${id}`
    })
}

/**
 * 创建 HTTP 代理
 * @param data 创建参数
 * @returns 响应结果
 */
export function fetchCreateHttpProxy(data: Api.Proxy.HttpProxyCreateParam) {
    return request.post({
        url: '/api/proxies/http',
        data,
        showSuccessMessage: true
    })
}

/**
 * 创建 HTTPS 代理
 * @param data 创建参数
 * @returns 响应结果
 */
export function fetchCreateHttpsProxy(data: Api.Proxy.HttpsProxyCreateParam) {
    return request.post({
        url: '/api/proxies/https',
        data,
        showSuccessMessage: true
    })
}

/**
 * 创建 TCP 代理
 * @param data 创建参数
 * @returns 响应结果
 */
export function fetchCreateTcpProxy(data: Api.Proxy.TcpProxyCreateParam) {
    return request.post({
        url: '/api/proxies/tcp',
        data,
        showSuccessMessage: true
    })
}

/**
 * 创建 UDP 代理
 */
export function fetchCreateUdpProxy(data: Api.Proxy.UdpProxyCreateParam) {
    return request.post({
        url: '/api/proxies/udp',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新 HTTP 代理
 * @param data 更新参数
 * @returns 响应结果
 */
export function fetchUpdateHttpProxy(data: Api.Proxy.HttpProxyUpdateParam) {
    return request.put({
        url: '/api/proxies/http',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新 HTTPS 代理
 * @param data 更新参数
 * @returns 响应结果
 */
export function fetchUpdateHttpsProxy(data: Api.Proxy.HttpsProxyUpdateParam) {
    return request.put({
        url: '/api/proxies/https',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新 TCP 代理
 * @param data 更新参数
 * @returns 响应结果
 */
export function fetchUpdateTcpProxy(data: Api.Proxy.TcpProxyUpdateParam) {
    return request.put({
        url: '/api/proxies/tcp',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新 UDP 代理
 */
export function fetchUpdateUdpProxy(data: Api.Proxy.UdpProxyUpdateParam) {
    return request.put({
        url: '/api/proxies/udp',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新代理状态
 * @param id 代理 ID
 * @param data 状态更新参数
 * @returns 响应结果
 */
export function fetchUpdateProxyStatus(id: string, data: Api.Proxy.ProxyStatusUpdateParam) {
    return request.put({
        url: `/api/proxies/status/${id}`,
        data,
        showSuccessMessage: true
    })
}

/**
 * 批量删除代理
 * @param data 删除参数
 * @returns 响应结果
 */
export function fetchBatchDeleteProxy(data: Api.Proxy.ProxyBatchDeleteParam) {
    return request.del({
        url: '/api/proxies',
        data,
        showSuccessMessage: true
    })
}

/**
 * 保存代理负载均衡配置
 */
export function fetchSaveProxyClusterConfig(
    proxyId: string,
    data: {
        targets: Api.Proxy.ProxyTargetAddParam[]
        loadBalance: Api.Proxy.LoadBalanceParam
    }
) {
    return request.put({
        url: `/api/proxies/${proxyId}/cluster`,
        data,
        showSuccessMessage: true
    })
}
