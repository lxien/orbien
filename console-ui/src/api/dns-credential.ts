import request from '@/utils/http'

export function fetchDnsCredentialList() {
  return request.get<Api.DnsCredential.CredentialDTO[]>({
    url: '/api/dns-credential/list'
  })
}

export function fetchDnsProviderSchemas() {
  return request.get<Api.DnsCredential.ProviderSchema[]>({
    url: '/api/dns-credential/providers'
  })
}

export function fetchSaveDnsCredential(params: Api.DnsCredential.SaveParams) {
  return request.post<Api.DnsCredential.CredentialDTO>({
    url: '/api/dns-credential',
    data: params
  })
}

export function fetchDeleteDnsCredential(id: number) {
  return request.del({
    url: `/api/dns-credential/${id}`
  })
}

export function fetchTestDnsCredential(id: number) {
  return request.post({
    url: `/api/dns-credential/${id}/test`
  })
}
