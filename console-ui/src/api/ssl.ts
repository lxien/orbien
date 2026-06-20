import request from '@/utils/http'

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