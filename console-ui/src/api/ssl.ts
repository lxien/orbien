import request from '@/utils/http'

/**
 * 获取SSL证书列表
 * @param params 分页参数
 * @returns 证书分页列表
 */
export function fetchGetCertListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Ssl.CertDTO>>({
    url: '/api/ssl-cert',
    params
  })
}

/**
 * 上传SSL证书
 * @param params 证书参数
 * @returns 证书DTO
 */
export function fetchSaveCert(params: Api.Ssl.CertSaveParams) {
  return request.post<Api.Ssl.CertDTO>({
    url: '/api/ssl-cert/save-cert',
    data: params
  })
}
/**
 * 删除SSL证书
 * @param ids 证书ID列表
 */
export function fetchDeleteCert(ids: string[]) {
  return request.del({
    url: '/api/ssl-cert',
    data: ids
  })
}

/**
 * 下载SSL证书
 * @param certId 证书ID
 * @returns 证书文件二进制流
 */
export function fetchDownloadCert(certId: string): Promise<Blob> {
  return request
    .get({
      url: `/api/ssl-cert/download-cert/${certId}`,
      responseType: 'blob',
      showErrorMessage: false
    })
    .then((res) => (res as { data: Blob }).data)
}

/**
 * 保存并部署SSL证书
 * @param params 证书参数
 */
export function fetchSaveAndDeployCert(params: Api.Ssl.CertSaveAndDeployParams) {
  return request.post<Api.CertBinding.BindResult>({
    url: '/api/ssl-cert/save-and-deploy',
    data: params
  })
}
