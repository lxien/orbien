import request from '@/utils/http'

/**
 * 获取SSL证书列表（分页）
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
 * @param params 证书参数（密钥和完整证书链）
 * @returns 证书DTO
 */
export function fetchSaveCert(params: Api.Ssl.CertSaveParams) {
  return request.post<Api.Ssl.CertDTO>({
    url: '/api/ssl-cert/save-cert',
    data: params
  })
}
/**
 * 删除SSL证书（支持批量）
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
  return request.get({
    url: `/api/ssl-cert/download-cert/${certId}`,
    responseType: 'blob',
    showErrorMessage: false
  }).then(res => (res as { data: Blob }).data)
}
