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
 * 获取 SOCKS5 认证详情
 * @param proxyId 代理 ID
 * @returns SOCKS5 认证详情
 */
export function fetchGetSocks5Auth(proxyId: string) {
  return request.get({
    url: `/api/socks5-auth/${proxyId}`
  })
}

/**
 * 更新 SOCKS5 认证配置
 * @param data 更新数据
 * @returns 操作结果
 */
export function fetchUpdateSocks5Auth(data: { proxyId: string; enabled: boolean }) {
  return request.put({
    url: '/api/socks5-auth',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 添加 SOCKS5 用户
 * @param data 用户数据
 * @returns 操作结果
 */
export function fetchAddSocks5AuthUser(data: {
  proxyId: string
  username: string
  password: string
}) {
  return request.post({
    url: '/api/socks5-auth/user',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 更新 SOCKS5 用户
 * @param data 用户数据
 * @returns 操作结果
 */
export function fetchUpdateSocks5AuthUser(data: { id: number; username: string; password: string }) {
  return request.put({
    url: '/api/socks5-auth/user',
    params: data,
    showSuccessMessage: true
  })
}

/**
 * 删除 SOCKS5 用户
 * @param id 用户 ID
 * @returns 操作结果
 */
export function fetchDeleteSocks5AuthUser(id: number) {
  return request.del({
    url: `/api/socks5-auth/user/${id}`,
    showSuccessMessage: true
  })
}
