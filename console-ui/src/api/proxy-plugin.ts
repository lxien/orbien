import {ProtocolType, getProtocolLabel} from '@/enums/orbien/business'
import {DomainType} from '@/enums/orbien/business'
import type {ProxyConfigProtocol} from '@/views/orbien/plugin/menus'
import {
    fetchGetHttpProxyById,
    fetchGetHttpsProxyById,
    fetchGetTcpProxyById,
    fetchGetUdpProxyById,
    fetchSaveProxyClusterConfig,
    fetchUpdateHttpProxy,
    fetchUpdateHttpsProxy,
    fetchUpdateTcpProxy,
    fetchUpdateUdpProxy
} from './proxy'

export type ProxyDetail =
    | Api.Proxy.HttpProxyDetailDTO
    | Api.Proxy.HttpsProxyDetailDTO
    | Api.Proxy.TcpProxyDetailDTO
    | Api.Proxy.UdpProxyDetailDTO

type HttpLikeDetail = Api.Proxy.HttpProxyDetailDTO | Api.Proxy.HttpsProxyDetailDTO

const GET_API = {
    [ProtocolType.HTTP]: fetchGetHttpProxyById,
    [ProtocolType.HTTPS]: fetchGetHttpsProxyById,
    [ProtocolType.TCP]: fetchGetTcpProxyById,
    [ProtocolType.UDP]: fetchGetUdpProxyById
}

export function fetchProxyDetail(protocol: ProxyConfigProtocol, id: string) {
    return GET_API[protocol](id) as Promise<ProxyDetail>
}

function isHttpLikeDetail(detail: ProxyDetail): detail is HttpLikeDetail {
    return 'domainType' in detail
}

function buildHttpDomainPayload(detail: HttpLikeDetail) {
    if (detail.domainType === DomainType.SUBDOMAIN) {
        return {
            subdomainBindings: (detail.subdomainBindings ?? []).map(({rootDomainId, prefix}) => ({
                rootDomainId,
                prefix
            }))
        }
    }
    if (detail.domainType === DomainType.CUSTOM_DOMAIN) {
        return {customDomains: detail.customDomains ?? []}
    }
    return {}
}

function buildTcpUpdatePayload(
    detail: Api.Proxy.TcpProxyDetailDTO,
    limitTotal?: number
): Api.Proxy.TcpProxyUpdateParam {
    return {
        id: detail.id,
        name: detail.name,
        localHost: detail.localHost,
        localPort: detail.localPort,
        ...(detail.remotePort != null ? {remotePort: detail.remotePort} : {}),
        ...(limitTotal != null ? {limitTotal} : detail.limitTotal != null ? {limitTotal: detail.limitTotal} : {})
    }
}

function buildUdpUpdatePayload(
    detail: Api.Proxy.UdpProxyDetailDTO,
    limitTotal?: number
): Api.Proxy.UdpProxyUpdateParam {
    return {
        id: detail.id,
        name: detail.name,
        localHost: detail.localHost,
        localPort: detail.localPort,
        ...(detail.remotePort != null ? {remotePort: detail.remotePort} : {}),
        ...(limitTotal != null ? {limitTotal} : detail.limitTotal != null ? {limitTotal: detail.limitTotal} : {})
    }
}

async function submitHttpLikeUpdate(
    protocol: ProxyConfigProtocol,
    detail: HttpLikeDetail,
    overrides: Partial<{
        limitTotal: number | undefined
    }> = {}
) {
    const firstTarget = detail.targets?.[0]
    const commonPayload = {
        id: detail.id,
        name: detail.name,
        domainType: detail.domainType,
        localHost: firstTarget?.host ?? detail.localHost,
        localPort: firstTarget?.port ?? detail.localPort,
        limitTotal: overrides.limitTotal ?? detail.limitTotal ?? undefined,
        ...buildHttpDomainPayload(detail)
    }

    if (protocol === ProtocolType.HTTPS) {
        const httpsDetail = detail as Api.Proxy.HttpsProxyDetailDTO
        return fetchUpdateHttpsProxy({
            ...commonPayload,
            forceHttps: httpsDetail.forceHttps
        })
    }

    return fetchUpdateHttpProxy(commonPayload)
}

function rejectPluginSave(protocol: ProxyConfigProtocol, feature: string) {
    return Promise.reject(new Error(`${getProtocolLabel(protocol)} ${feature}暂未适配新接口`))
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
    protocol: ProxyConfigProtocol,
    detail: ProxyDetail,
    _transport: Api.Proxy.TransportSaveParam
) {
    if (!isHttpLikeDetail(detail)) {
        return rejectPluginSave(protocol, '传输配置')
    }
    return rejectPluginSave(protocol, '传输配置')
}

export function saveProxyBandwidthConfig(
    protocol: ProxyConfigProtocol,
    detail: ProxyDetail,
    bandwidth: Api.Proxy.BandwidthSaveParam
) {
    if (protocol === ProtocolType.TCP) {
        const tcpDetail = detail as Api.Proxy.TcpProxyDetailDTO
        const limitTotalMbps =
            bandwidth.limitTotal != null && bandwidth.unit
                ? Math.round(bandwidth.limitTotal / 1_000_000)
                : tcpDetail.limitTotal ?? undefined
        return fetchUpdateTcpProxy(buildTcpUpdatePayload(tcpDetail, limitTotalMbps))
    }

    if (protocol === ProtocolType.UDP) {
        const udpDetail = detail as Api.Proxy.UdpProxyDetailDTO
        const limitTotalMbps =
            bandwidth.limitTotal != null && bandwidth.unit
                ? Math.round(bandwidth.limitTotal / 1_000_000)
                : udpDetail.limitTotal ?? undefined
        return fetchUpdateUdpProxy(buildUdpUpdatePayload(udpDetail, limitTotalMbps))
    }

    if (!isHttpLikeDetail(detail)) {
        return rejectPluginSave(protocol, '带宽配置')
    }

    const limitTotalMbps =
        bandwidth.limitTotal != null && bandwidth.unit
            ? Math.round(bandwidth.limitTotal / 1_000_000)
            : detail.limitTotal ?? undefined

    return submitHttpLikeUpdate(protocol, detail, {limitTotal: limitTotalMbps})
}
