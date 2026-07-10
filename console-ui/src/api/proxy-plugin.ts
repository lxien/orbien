import {ProtocolType, getProtocolLabel, BANDWIDTH_UNIT_TO_BPS, BandwidthUnit} from '@/enums/orbien/business'
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
import {fetchGetFileShareById, fetchUpdateFileShare} from './file-share'
import {saveProxyTransport} from './proxy-transport'

export type ProxyDetail =
    | Api.Proxy.HttpProxyDetailDTO
    | Api.Proxy.HttpsProxyDetailDTO
    | Api.Proxy.TcpProxyDetailDTO
    | Api.Proxy.UdpProxyDetailDTO
    | Api.FileShare.FileShareDetailDTO

type HttpLikeDetail = Api.Proxy.HttpProxyDetailDTO | Api.Proxy.HttpsProxyDetailDTO

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

function isHttpLikeDetail(detail: ProxyDetail): detail is HttpLikeDetail {
    return 'localHost' in detail
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

function buildFileShareUpdatePayload(
    detail: Api.FileShare.FileShareDetailDTO,
    overrides: Partial<{
        limitTotal: number | undefined
        limitIn: number | null
        limitOut: number | null
    }> = {}
): Api.FileShare.FileShareUpdateParam {
    const payload: Api.FileShare.FileShareUpdateParam = {
        id: detail.id,
        name: detail.name,
        domainType: detail.domainType,
        rootPath: detail.rootPath,
        authEnabled: detail.authEnabled,
        authUsers: detail.authUsers?.map((user) => ({
            id: user.id,
            username: user.username,
            permission: user.permission
        })),
        maxUploadSize: detail.maxUploadSize,
        allowUpload: detail.allowUpload,
        allowDelete: detail.allowDelete,
        allowMkdir: detail.allowMkdir,
        allowMove: detail.allowMove,
        allowRename: detail.allowRename
    }

    if (detail.domainType === DomainType.SUBDOMAIN) {
        payload.subdomainBindings = (detail.subdomainBindings ?? []).map(({rootDomainId, prefix}) => ({
            rootDomainId,
            prefix
        }))
    } else if (detail.domainType === DomainType.CUSTOM_DOMAIN) {
        payload.customDomains = detail.customDomains ?? []
    }

    if (overrides.limitTotal !== undefined) {
        payload.limitTotal = overrides.limitTotal
    } else if (detail.limitTotal != null) {
        payload.limitTotal = detail.limitTotal
    }

    if (overrides.limitIn !== undefined) {
        payload.limitIn = overrides.limitIn ?? undefined
    }
    if (overrides.limitOut !== undefined) {
        payload.limitOut = overrides.limitOut ?? undefined
    }

    return payload
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
    _protocol: ProxyConfigProtocol,
    _detail: ProxyDetail,
    transport: Api.Proxy.TransportSaveParam,
    proxyId: string
) {
    return saveProxyTransport(proxyId, transport)
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

    if (protocol === ProtocolType.FILE) {
        const fileDetail = detail as Api.FileShare.FileShareDetailDTO
        const unit = bandwidth.unit ?? BandwidthUnit.MBPS
        const toBps = (value?: number | null) =>
            value != null && value > 0 ? Math.round(value * BANDWIDTH_UNIT_TO_BPS[unit]) : null
        // @ts-ignore
        const limitTotalMbps =
            bandwidth.limitTotal != null && bandwidth.unit
                ? Math.round(bandwidth.limitTotal / BANDWIDTH_UNIT_TO_BPS[unit])
                : fileDetail.limitTotal ?? undefined
        return fetchUpdateFileShare(
            buildFileShareUpdatePayload(fileDetail, {
                limitTotal: limitTotalMbps,
                limitIn: toBps(bandwidth.limitIn),
                limitOut: toBps(bandwidth.limitOut)
            })
        )
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
