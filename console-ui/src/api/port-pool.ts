import request from '@/utils/http'

/**
 * 获取端口池列表（分页）
 */
export function fetchGetPortPoolListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.PortPool.PortPoolDTO>>({
    url: '/api/port-pools',
    params
  })
}

/**
 * 根据 ID 获取端口池详情
 */
export function fetchGetPortPoolById(id: number) {
  return request.get<Api.PortPool.PortPoolDTO>({
    url: `/api/port-pools/${id}`
  })
}

/**
 * 创建端口池配置
 */
export function fetchCreatePortPool(params: Api.PortPool.PortPoolCreateParam) {
  return request.post<Api.PortPool.PortPoolDTO>({
    url: '/api/port-pools',
    data: params
  })
}

/**
 * 更新端口池配置
 */
export function fetchUpdatePortPool(params: Api.PortPool.PortPoolUpdateParam) {
  return request.put({
    url: '/api/port-pools',
    data: params
  })
}

/**
 * 批量删除端口池配置
 */
export function fetchDeleteBatchPortPools(ids: number[]) {
  return request.del({
    url: '/api/port-pools',
    data: { ids }
  })
}
