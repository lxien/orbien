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

declare namespace Api.HeaderRewrite {
  interface RuleDTO {
    id: number
    direction: number
    action: number
    name: string
    value?: string | null
  }

  interface DetailDTO {
    proxyId: string
    enabled: boolean
    requestRules: RuleDTO[]
    responseRules: RuleDTO[]
  }

  interface UpdateParam {
    proxyId: string
    enabled: boolean
  }

  interface RuleAddParam {
    proxyId: string
    direction: number
    action: number
    name: string
    value?: string | null
  }

  interface RuleUpdateParam {
    id: number
    proxyId: string
    direction: number
    action: number
    name: string
    value?: string | null
  }
}
