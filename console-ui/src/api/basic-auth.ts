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
 * 获取 Basic Auth 详情
 * @param proxyId 代理 ID
 * @returns Basic Auth 详情
 */
export function fetchGetBasicAuth(proxyId: string) {
  return request.get({
    url: `/api/basic-auth/${proxyId}`
  })
}

/**
 * 更新 Basic Auth 配置
 * @param data 更新数据
 * @returns 操作结果
 */
export function fetchUpdateBasicAuth(data: { proxyId: string; enabled: boolean }) {
  return request.put({
    url: '/api/basic-auth',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 添加 HTTP 用户
 * @param data 用户数据
 * @returns 操作结果
 */
export function fetchAddBasicAuthUser(data: {
  proxyId: string
  username: string
  password: string
}) {
  return request.post({
    url: '/api/basic-auth/user',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 更新 HTTP 用户
 * @param data 用户数据
 * @returns 操作结果
 */
export function fetchUpdateBasicAuthUser(data: { id: number; username: string; password: string }) {
  return request.put({
    url: '/api/basic-auth/user',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 删除 HTTP 用户
 * @param id 用户 ID
 * @returns 操作结果
 */
export function fetchDeleteBasicAuthUser(id: number) {
  return request.del({
    url: `/api/basic-auth/user/${id}`,
    showSuccessMessage: true
  })
}
