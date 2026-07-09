import request from '@/utils/http'

export function fetchProxyTransport(proxyId: string) {
  return request.get<Api.Proxy.ProxyTransportDetailDTO>({
    url: `/api/proxy-transport/${proxyId}`
  })
}

export function saveProxyTransport(proxyId: string, data: Api.Proxy.TransportSaveParam) {
  return request.put({
    url: `/api/proxy-transport/${proxyId}`,
    data
  })
}
