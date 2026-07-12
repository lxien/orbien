import request from '@/utils/http'

export const INSPECTOR_DISPLAY_LIMIT = 50

export function fetchInspectorRequests(proxyId: string, limit = INSPECTOR_DISPLAY_LIMIT) {
    return request.get<Api.Inspector.RecordSummary[]>({
        url: '/api/inspector/requests',
        params: {proxyId, limit}
    })
}

export function fetchInspectorRequestDetail(id: string) {
    return request.get<Api.Inspector.RecordDetail>({
        url: `/api/inspector/requests/${id}`
    })
}

export function fetchClearInspectorRequests(proxyId: string) {
    return request.del({
        url: '/api/inspector/requests',
        params: {proxyId}
    })
}

export function fetchInspectorConfig(proxyId: string) {
    return request.get<Api.Inspector.Config>({
        url: '/api/inspector/config',
        params: {proxyId}
    })
}

export function fetchUpdateInspectorConfig(data: Api.Inspector.ConfigUpdateParam) {
    return request.put<Api.Inspector.Config>({
        url: '/api/inspector/config',
        data
    })
}

export function buildInspectorStreamUrl(proxyId: string, token?: string) {
    const base = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
    const params = new URLSearchParams({proxyId})
    if (token) {
        params.set('token', token)
    }
    return `${base}/api/inspector/stream?${params.toString()}`
}
