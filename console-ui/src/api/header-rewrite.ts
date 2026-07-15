/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import request from '@/utils/http'

/**
 * 获取 Header 改写详情
 * @param proxyId 代理 ID
 * @returns Header 改写详情
 */
export function fetchGetHeaderRewrite(proxyId: string) {
  return request.get<Api.HeaderRewrite.DetailDTO>({
    url: `/api/header-rewrite/${proxyId}`
  })
}

/**
 * 更新 Header 改写开关
 * @param data 更新数据
 * @returns 操作结果
 */
export function fetchUpdateHeaderRewrite(data: Api.HeaderRewrite.UpdateParam) {
  return request.put({
    url: '/api/header-rewrite',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 添加 Header 改写规则
 * @param data 规则数据
 * @returns 操作结果
 */
export function fetchAddHeaderRewriteRule(data: Api.HeaderRewrite.RuleAddParam) {
  return request.post({
    url: '/api/header-rewrite/rule',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 更新 Header 改写规则
 * @param data 规则数据
 * @returns 操作结果
 */
export function fetchUpdateHeaderRewriteRule(data: Api.HeaderRewrite.RuleUpdateParam) {
  return request.put({
    url: '/api/header-rewrite/rule',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 删除 Header 改写规则
 * @param id 规则 ID
 * @returns 操作结果
 */
export function fetchDeleteHeaderRewriteRule(id: number) {
  return request.del({
    url: `/api/header-rewrite/rule/${id}`,
    showSuccessMessage: true
  })
}
