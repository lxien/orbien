/**
 * 代理配置弹窗 - 协议类型与菜单配置
 */
import {ProtocolType} from '@/enums/orbien/business'

/** 弹窗支持的协议类型 */
export type ProxyConfigProtocol =
    | ProtocolType.TCP
    | ProtocolType.UDP
    | ProtocolType.HTTP
    | ProtocolType.HTTPS
    | ProtocolType.SOCKS5
    | ProtocolType.FILE

export interface ProxyConfigMenuItem {
    key: string
    label: string
    icon: string
}

const commonMenus = {
    access: {key: 'access', label: '访问控制', icon: 'ri:shield-line'},
    time: {key: 'time', label: '时间限制', icon: 'ri:time-line'},
    auth: {key: 'auth', label: '认证鉴权', icon: 'ri:key-line'},
    load: {key: 'load', label: '负载均衡', icon: 'ri:server-line'},
    health: {key: 'health', label: '健康检查', icon: 'ri:heart-pulse-line'},
    tls: {key: 'tls', label: 'TLS 加密', icon: 'ri:shield-check-line'},
    trans: {key: 'trans', label: '传输安全', icon: 'ri:lock-line'},
    limit: {key: 'limit', label: '流量限制', icon: 'ri:speed-line'},
    headers: {key: 'headers', label: 'Header 改写', icon: 'ri:edit-box-line'}
} as const satisfies Record<string, ProxyConfigMenuItem>

/** 各协议对应的侧边栏菜单 */
export const protocolMenuMap: Record<ProxyConfigProtocol, ProxyConfigMenuItem[]> = {
    [ProtocolType.TCP]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.load,
        commonMenus.trans,
        commonMenus.health,
        commonMenus.limit
    ],
    [ProtocolType.UDP]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.load,
        commonMenus.trans,
        commonMenus.limit
    ],
    [ProtocolType.HTTP]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.auth,
        commonMenus.load,
        commonMenus.trans,
        commonMenus.health,
        commonMenus.limit,
        commonMenus.headers
    ],
    [ProtocolType.HTTPS]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.auth,
        commonMenus.load,
        commonMenus.tls,
        commonMenus.health,
        commonMenus.trans,
        commonMenus.limit,
        commonMenus.headers
    ],
    [ProtocolType.SOCKS5]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.trans,
        commonMenus.limit
    ],
    [ProtocolType.FILE]: [
        commonMenus.access,
        commonMenus.time,
        commonMenus.tls,
        commonMenus.trans,
        commonMenus.limit
    ]
}
export function getProtocolMenus(protocol: ProxyConfigProtocol): ProxyConfigMenuItem[] {
    return protocolMenuMap[protocol] ?? protocolMenuMap[ProtocolType.HTTP]
}
