import request from '@/utils/http'

/**
 * 获取SSL证书列表（分页）
 * @param params 分页参数
 * @returns 证书分页列表
 */
export function fetchGetCertListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Ssl.CertDTO>>({
    url: '/api/ssl-certificate',
    params
  })
}

/**
 * 保存SSL证书
 * @param params 证书参数（密钥和完整证书链）
 * @returns 证书DTO
 */
export function fetchSaveCert(params: Api.Ssl.CertSaveParams) {
  return request.post<Api.Ssl.CertDTO>({
    url: '/api/ssl-certificate/save-cert',
    data: params
  })
}