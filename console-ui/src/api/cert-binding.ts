import request from '@/utils/http'

export function fetchBindCert(params: Api.CertBinding.BindParams) {
  return request.post<Api.CertBinding.BindResult>({
    url: '/api/cert-binding/bind',
    data: params
  })
}

export function fetchPreviewBind(params: Api.CertBinding.PreviewParams) {
  return request.post<Api.CertBinding.PreviewItem[]>({
    url: '/api/cert-binding/preview',
    data: params
  })
}

export function fetchListBindableDomains(certId: string) {
  return request.get<Api.CertBinding.PreviewItem[]>({
    url: '/api/cert-binding/bindable-domains',
    params: { certId }
  })
}

export function fetchGetProxyCertMatrix(proxyId: string) {
  return request.get<Api.CertBinding.ProxyCertMatrix>({
    url: `/api/cert-binding/proxy/${proxyId}`
  })
}

export function fetchGetCertUsage(certId: string) {
  return request.get<Api.CertBinding.CertUsage>({
    url: `/api/cert-binding/cert/${certId}`
  })
}

export function fetchDisableBinding(bindingId: number) {
  return request.put({
    url: `/api/cert-binding/${bindingId}/disable`
  })
}

export function fetchEnableBinding(bindingId: number) {
  return request.put({
    url: `/api/cert-binding/${bindingId}/enable`
  })
}

export function fetchUnbindBinding(bindingId: number) {
  return request.del({
    url: `/api/cert-binding/${bindingId}`
  })
}

export function fetchRebindCert(bindingId: number, certId: string) {
  return request.put({
    url: `/api/cert-binding/${bindingId}/rebind`,
    data: { certId }
  })
}

export function fetchRedeployBinding(bindingId: number) {
  return request.post({
    url: `/api/cert-binding/${bindingId}/redeploy`
  })
}

export function fetchDisableAllBindingsByProxy(proxyId: string) {
  return request.put({
    url: `/api/cert-binding/proxy/${proxyId}/disable-all`
  })
}
