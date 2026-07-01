import request from '@/utils/http'

/**
 * 获取域名列表（分页）
 * @param params 搜索参数
 * @returns 域名分页列表
 */
export function fetchGetDomainListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Domain.DomainDTO>>({
    url: '/api/domains',
    params
  })
}

/**
 * 根据ID获取域名详情
 * @param id 域名ID
 * @returns 域名详情
 */
export function fetchGetDomainById(id: number) {
  return request.get<Api.Domain.DomainDTO>({
    url: `/api/domains/${id}`
  })
}

/**
 * 获取已用域名列表（分页）
 * @param params 搜索参数
 * @returns 已用域名分页列表
 */
export function fetchGetUsedDomainListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Domain.UsedDomainDTO>>({
    url: '/api/domains/used',
    params
  })
}
