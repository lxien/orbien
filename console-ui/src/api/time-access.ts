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

export function fetchGetTimeAccess(proxyId: string) {
  return request.get<Api.TimeAccess.DetailDTO>({
    url: `/api/time-access/${proxyId}`
  })
}

export function fetchUpdateTimeAccess(data: Api.TimeAccess.UpdateParam) {
  return request.put({
    url: '/api/time-access',
    params: data,
    showSuccessMessage: true
  })
}

export function fetchAddTimeAccessWindow(data: Api.TimeAccess.WindowAddParam) {
  return request.post({
    url: '/api/time-access/window',
    params: data,
    showSuccessMessage: true
  })
}

export function fetchUpdateTimeAccessWindow(data: Api.TimeAccess.WindowUpdateParam) {
  return request.put({
    url: '/api/time-access/window',
    params: data,
    showSuccessMessage: true
  })
}

export function fetchDeleteTimeAccessWindow(id: number) {
  return request.del({
    url: `/api/time-access/window/${id}`,
    showSuccessMessage: true
  })
}
