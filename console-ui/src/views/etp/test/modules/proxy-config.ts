/**
 * 代理配置弹窗 - 协议类型与菜单配置
 */
import { ProtocolType } from '@/enums/businessEnum'

/** 弹窗支持的协议类型 */
export type ProxyConfigProtocol = ProtocolType.TCP | ProtocolType.HTTP | ProtocolType.HTTPS

export interface ProxyConfigMenuItem {
  key: string
  label: string
  icon: string
}

const commonMenus = {
  access: { key: 'access', label: '访问控制', icon: 'ri:shield-line' },
  auth: { key: 'auth', label: '认证鉴权', icon: 'ri:key-line' },
  load: { key: 'load', label: '集群代理', icon: 'ri:server-line' },
  ssl: { key: 'ssl', label: 'SSL加密', icon: 'ri:shield-check-line' },
  trans: { key: 'trans', label: '传输安全', icon: 'ri:lock-line' },
  limit: { key: 'limit', label: '流量限制', icon: 'ri:speed-line' }
} as const satisfies Record<string, ProxyConfigMenuItem>

/** 各协议对应的侧边栏菜单 */
export const protocolMenuMap: Record<ProxyConfigProtocol, ProxyConfigMenuItem[]> = {
  [ProtocolType.TCP]: [
    commonMenus.access,
    commonMenus.load,
    commonMenus.trans,
    commonMenus.limit
  ],
  [ProtocolType.HTTP]: [
    commonMenus.access,
    commonMenus.auth,
    commonMenus.load,
    commonMenus.trans,
    commonMenus.limit
  ],
  [ProtocolType.HTTPS]: [
    commonMenus.access,
    commonMenus.auth,
    commonMenus.load,
    commonMenus.ssl,
    commonMenus.trans,
    commonMenus.limit
  ]
}

/** 各协议弹窗标题 */
export const protocolTitleMap: Record<ProxyConfigProtocol, string> = {
  [ProtocolType.TCP]: 'TCP 代理配置',
  [ProtocolType.HTTP]: 'HTTP 代理配置',
  [ProtocolType.HTTPS]: 'HTTPS 代理配置'
}

export function getProtocolMenus(protocol: ProxyConfigProtocol): ProxyConfigMenuItem[] {
  return protocolMenuMap[protocol] ?? protocolMenuMap[ProtocolType.HTTP]
}

export function getProtocolTitle(protocol: ProxyConfigProtocol): string {
  return protocolTitleMap[protocol] ?? protocolTitleMap[ProtocolType.HTTP]
}
