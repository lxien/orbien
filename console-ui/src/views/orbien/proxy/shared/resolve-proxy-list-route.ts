import {ProtocolType} from '@/enums/orbien/business'
import type {RouteLocationRaw} from 'vue-router'

const PROTOCOL_LIST_ROUTE: Partial<Record<ProtocolType, RouteLocationRaw>> = {
    [ProtocolType.HTTP]: {name: 'HTTP'},
    [ProtocolType.HTTPS]: {name: 'HTTPS'},
    [ProtocolType.TCP]: {name: 'TCP'},
    [ProtocolType.UDP]: {name: 'UDP'},
    [ProtocolType.SOCKS5]: {name: 'SOCKS5'},
    [ProtocolType.FILE]: {name: 'FileShare'}
}

export function resolveProxyListRoute(protocol?: number | null): RouteLocationRaw | null {
    if (protocol == null) return null
    return PROTOCOL_LIST_ROUTE[protocol as ProtocolType] ?? null
}
