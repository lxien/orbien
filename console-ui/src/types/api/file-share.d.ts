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

declare namespace Api.FileShare {
    /** 文件共享列表项 */
    interface FileShareListDTO extends Api.Proxy.ProxyListDTO {
        domains: string[]
        accessUrls: string[]
        rootPath: string
        authEnabled: boolean
        authUserCount: number
        httpsProxyPort?: number
        tlsCertSummary?: Api.Proxy.TlsCertSummary
    }

    /** 认证用户 */
    interface FileShareUserDTO {
        id: number
        username: string
        permission: string
    }

    /** 认证用户参数 */
    interface FileShareAuthUserParam {
        id?: number
        username: string
        password?: string
        /** read | read_write */
        permission: string
    }

    /** 文件共享详情 */
    interface FileShareDetailDTO {
        id: string
        agentId: string
        name: string
        domainType: number
        customDomains?: string[]
        subdomainBindings?: Api.Proxy.SubdomainBindingDTO[]
        domains?: string[]
        accessUrls?: string[]
        rootPath: string
        maxUploadSize?: number
        allowUpload?: boolean
        allowDelete?: boolean
        allowMkdir?: boolean
        allowMove?: boolean
        allowRename?: boolean
        limitTotal?: number | null
        transportProtocol?: number
        transport?: Api.Proxy.TransportDTO
        bandwidth?: Api.Proxy.BandwidthDTO | null
        authEnabled?: boolean
        authUsers?: FileShareUserDTO[]
        createdAt?: string
        updatedAt?: string
    }

    /** 创建参数 */
    interface FileShareCreateParam {
        agentId: string
        name: string
        domainType: number
        subdomainBindings?: Api.Proxy.SubdomainBindingParam[]
        customDomains?: string[]
        rootPath: string
        limitTotal?: number | null
        authEnabled?: boolean
        authUsers?: FileShareAuthUserParam[]
        maxUploadSize?: number
        allowUpload?: boolean
        allowDelete?: boolean
        allowMkdir?: boolean
        allowMove?: boolean
        allowRename?: boolean
    }

    /** 更新参数 */
    interface FileShareUpdateParam {
        id: string
        name: string
        domainType: number
        subdomainBindings?: Api.Proxy.SubdomainBindingParam[]
        customDomains?: string[]
        rootPath: string
        limitTotal?: number | null
        limitIn?: number
        limitOut?: number
        authEnabled?: boolean
        authUsers?: FileShareAuthUserParam[]
        maxUploadSize?: number
        allowUpload?: boolean
        allowDelete?: boolean
        allowMkdir?: boolean
        allowMove?: boolean
        allowRename?: boolean
    }
}
