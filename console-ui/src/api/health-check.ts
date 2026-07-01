/*
 *    Copyright 2026 xiaoniucode
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
 * 获取健康检查配置
 * @param proxyId 代理 ID
 */
export function fetchGetHealthCheck(proxyId: string) {
  return request.get<Api.HealthCheck.HealthCheckDTO>({
    url: `/api/health-check/${proxyId}`
  })
}

/**
 * 保存健康检查配置
 * @param data 保存参数
 */
export function fetchSaveHealthCheck(data: Api.HealthCheck.HealthCheckSaveParam) {
  return request.put({
    url: '/api/health-check',
    data,
    showSuccessMessage: true
  })
}

/**
 * 切换健康检查启用状态
 * @param data 状态参数
 */
export function fetchUpdateHealthCheckStatus(data: Api.HealthCheck.HealthCheckStatusUpdateParam) {
  return request.put({
    url: '/api/health-check/status',
    data,
    showSuccessMessage: true
  })
}
