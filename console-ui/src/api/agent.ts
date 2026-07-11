import request from '@/utils/http'

/**
 * 获取客户端列表（分页）
 * @param params 分页参数
 * @returns 客户端分页列表
 */
export function fetchGetAgentListByPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.Agent.AgentDTO>>({
    url: '/api/agents/list-by-page',
    params
  })
}

/**
 * 获取标准客户端列表（代理绑定）
 */
export function fetchGetAgentsForProxySelection(includeId?: string) {
  return request.get<Api.Agent.AgentDTO[]>({
    url: '/api/agents/list-for-proxy',
    params: includeId ? { includeId } : undefined
  })
}

/**
 * 获取所有客户端列表
 * @returns 客户端列表
 */
export function fetchGetAgentListAll() {
  return request.get<Api.Agent.AgentDTO[]>({
    url: '/api/agents/list'
  })
}

/**
 * 获取单个客户端详情
 * @param id 客户端ID
 * @returns 客户端详情
 */
export function fetchGetAgentById(id: string) {
  return request.get<Api.Agent.AgentDTO>({
    url: `/api/agents/${id}`
  })
}

/**
 * 剔除在线客户端
 * @param id 客户端ID
 * @returns 剔除结果
 */
export function fetchKickoutAgent(id: string) {
  return request.put({
    url: `/api/agents/kickout/${id}`
  })
}

/**
 * 批量删除客户端
 * @param ids 客户端ID列表
 * @returns 删除结果
 */
export function fetchDeleteBatchAgents(ids: string[]) {
  return request.del({
    url: '/api/agents',
    data: { ids }
  })
}
