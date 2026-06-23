/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

import request from '@/utils/http'

/**
 * 获取SSL部署信息
 * @param proxyId 代理ID
 * @returns SSL部署信息
 */
export function fetchGetSslDeployInfo(proxyId: string) {
  return request.get<Api.Deploy.SslDeployInfoDTO>({
    url: `/api/certificate-deployment/get-ssl/${proxyId}`
  })
}

/**
 * 关闭SSL证书
 * @param proxyId 代理ID
 */
export function fetchCloseSsl(proxyId: string) {
  return request.put({
    url: `/api/certificate-deployment/close-ssl/${proxyId}`
  })
}