import {ProtocolType, getProtocolLabel} from '@/enums/orbien/business'
import type {ProxyConfigProtocol} from '@/views/orbien/plugin/menus'
import {
    fetchGetHttpProxyById,
    fetchGetHttpsProxyById,
    fetchGetTcpProxyById,
    fetchGetUdpProxyById,
    fetchSaveProxyClusterConfig,
    fetchUpdateProxyBandwidth
} from './proxy'
import {fetchGetFileShareById} from './file-share'
import {saveProxyTransport} from './proxy-transport'

export type ProxyDetail =
    | Api.Proxy.HttpProxyDetailDTO
    | Api.Proxy.HttpsProxyDetailDTO
    | Api.Proxy.TcpProxyDetailDTO
    | Api.Proxy.UdpProxyDetailDTO
    | Api.FileShare.FileShareDetailDTO

const GET_API = {
    [ProtocolType.HTTP]: fetchGetHttpProxyById,
    [ProtocolType.HTTPS]: fetchGetHttpsProxyById,
    [ProtocolType.TCP]: fetchGetTcpProxyById,
    [ProtocolType.UDP]: fetchGetUdpProxyById,
    [ProtocolType.FILE]: fetchGetFileShareById
} as const

export function fetchProxyDetail(protocol: ProxyConfigProtocol, id: string) {
    const fetcher = GET_API[protocol as keyof typeof GET_API]
    if (!fetcher) {
        return Promise.reject(new Error(`${getProtocolLabel(protocol)} 暂不支持读取详情`))
    }
    return fetcher(id) as Promise<ProxyDetail>
}

export function saveProxyClusterConfig(
    _protocol: ProxyConfigProtocol,
    detail: ProxyDetail,
    targets: Api.Proxy.ProxyTargetAddParam[],
    loadBalance: Api.Proxy.LoadBalanceParam
) {
    return fetchSaveProxyClusterConfig(detail.id, {targets, loadBalance})
}

export function saveProxyTransportConfig(
    _protocol: ProxyConfigProtocol,
    _detail: ProxyDetail,
    transport: Api.Proxy.TransportSaveParam,
    proxyId: string
) {
    return saveProxyTransport(proxyId, transport)
}

export function saveProxyBandwidthConfig(
    _protocol: ProxyConfigProtocol,
    detail: ProxyDetail,
    bandwidth: Api.Proxy.BandwidthSaveParam
) {
    return fetchUpdateProxyBandwidth(detail.id, bandwidth)
}
