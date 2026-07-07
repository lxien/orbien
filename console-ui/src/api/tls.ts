import request from '@/utils/http'

/**
 * 获取TLS证书列表
 * @param params 分页参数
 * @returns 证书分页列表
 */
export function fetchGetCertListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Tls.CertDTO>>({
    url: '/api/tls-cert',
    params
  })
}

/**
 * 上传TLS 证书
 * @param params 证书参数
 * @returns 证书DTO
 */
export function fetchSaveCert(params: Api.Tls.CertSaveParams) {
  return request.post<Api.Tls.CertDTO>({
    url: '/api/tls-cert/save-cert',
    data: params
  })
}
/**
 * 删除TLS 证书
 * @param ids 证书ID列表
 */
export function fetchDeleteCert(ids: string[]) {
  return request.del({
    url: '/api/tls-cert',
    data: ids
  })
}

/**
 * 下载TLS 证书
 * @param certId 证书ID
 * @returns 证书文件二进制流
 */
export function fetchDownloadCert(certId: string): Promise<Blob> {
  return request
    .get({
      url: `/api/tls-cert/download-cert/${certId}`,
      responseType: 'blob',
      showErrorMessage: false
    })
    .then((res) => (res as { data: Blob }).data)
}

/**
 * 保存并部署TLS 证书
 * @param params 证书参数
 */
export function fetchSaveAndDeployCert(params: Api.Tls.CertSaveAndDeployParams) {
  return request.post<Api.CertBinding.BindResult>({
    url: '/api/tls-cert/save-and-deploy',
    data: params
  })
}

/**
 * 更新证书自动续签开关
 */
export function fetchUpdateCertAutoRenew(certId: string, autoRenew: boolean) {
  return request.put<Api.Tls.AutoRenewResult>({
    url: `/api/tls-cert/${certId}/auto-renew`,
    data: { autoRenew }
  })
}
