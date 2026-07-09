import request from '@/utils/http'

/**
 * 获取文件共享列表（分页）
 */
export function fetchGetFileShareList(params: Api.Common.CommonSearchParams) {
    return request.get<Api.Common.PaginatedResponse<Api.FileShare.FileShareListDTO>>({
        url: '/api/proxies/file',
        params
    })
}

/**
 * 获取文件共享详情
 */
export function fetchGetFileShareById(id: string) {
    return request.get<Api.FileShare.FileShareDetailDTO>({
        url: `/api/proxies/file/${id}`
    })
}

/**
 * 创建文件共享
 */
export function fetchCreateFileShare(data: Api.FileShare.FileShareCreateParam) {
    return request.post({
        url: '/api/proxies/file',
        data,
        showSuccessMessage: true
    })
}

/**
 * 更新文件共享
 */
export function fetchUpdateFileShare(data: Api.FileShare.FileShareUpdateParam) {
    return request.put({
        url: '/api/proxies/file',
        data,
        showSuccessMessage: true
    })
}
