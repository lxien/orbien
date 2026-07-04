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
 * 获取访问控制详情
 * @param proxyId 代理ID
 * @returns 访问控制详情
 */
export function fetchGetAccessControl(proxyId: string) {
  return request.get<any>({
    url: `/api/access-control/${proxyId}`
  })
}

/**
 * 更新访问控制配置
 * @param params 更新参数
 * @returns 更新结果
 */
export function fetchUpdateAccessControl(params: {
  proxyId: string
  enabled: boolean
  mode: number
}) {
  return request.put<any>({
    url: '/api/access-control',
    params,
    showSuccessMessage: true
  })
}

/**
 * 添加访问控制规则
 * @param params 添加参数
 * @returns 添加结果
 */
export function fetchAddAccessControlRule(params: {
  proxyId: string
  cidr: string
  ruleType: number
}) {
  return request.post<any>({
    url: '/api/access-control/rule',
    params,
    showSuccessMessage: true
  })
}

/**
 * 更新访问控制规则
 * @param params 更新参数
 * @returns 更新结果
 */
export function fetchUpdateAccessControlRule(params: {
  id: number
  cidr: string
  ruleType: number
}) {
  return request.put<any>({
    url: '/api/access-control/rule',
    params,
    showSuccessMessage: true
  })
}

/**
 * 删除访问控制规则
 * @param id 规则ID
 * @returns 删除结果
 */
export function fetchDeleteAccessControlRule(id: number) {
  return request.del<any>({
    url: `/api/access-control/rule/${id}`,
    showSuccessMessage: true
  })
}
