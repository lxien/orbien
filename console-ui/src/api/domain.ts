import request from '@/utils/http'

/**
 * 获取全部根域名列表（下拉选择用）
 * @returns 根域名列表
 */
export function fetchGetDomainListAll() {
  return request.get<Api.Domain.DomainDTO[]>({
    url: '/api/domains/list'
  })
}

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

/**
 * 创建根域名
 * @param params 创建参数
 * @returns 创建结果
 */
export function fetchCreateDomain(params: Api.Domain.DomainCreateParam) {
  return request.post<Api.Domain.DomainDTO>({
    url: '/api/domains',
    data: params
  })
}

/**
 * 更新根域名描述
 * @param params 更新参数
 * @returns 更新结果
 */
export function fetchUpdateDomain(params: Api.Domain.DomainUpdateParam) {
  return request.put({
    url: '/api/domains',
    data: params
  })
}

/**
 * 批量删除根域名
 * @param ids 域名ID列表
 * @returns 删除结果
 */
export function fetchDeleteBatchDomains(ids: number[]) {
  return request.del({
    url: '/api/domains',
    data: { ids }
  })
}
