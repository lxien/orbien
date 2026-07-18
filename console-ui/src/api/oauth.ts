import request from '@/utils/http'

/** 查询已启用的 OAuth Provider（登录页） */
export function fetchPublicOAuthProviders() {
    return request.get<Api.OAuth.PublicProvider[]>({
        url: '/api/auth/oauth/providers',
        showErrorMessage: false
    })
}

/** 使用一次性 ticket 兑换控制台 JWT */
export function fetchOAuthToken(ticket: string) {
    return request.post<Api.Auth.LoginResponse>({
        url: '/api/auth/oauth/token',
        params: {ticket}
    })
}

/** 查询全部 OAuth Provider 配置 */
export function fetchOAuthProviderConfigs() {
    return request.get<Api.OAuth.ProviderConfig[]>({
        url: '/api/oauth/providers'
    })
}

/** 保存 OAuth Provider 凭证配置 */
export function fetchSaveOAuthProvider(provider: string, params: Api.OAuth.ProviderSaveParam) {
    return request.put<Api.OAuth.ProviderConfig>({
        url: `/api/oauth/providers/${provider}`,
        params
    })
}

/** 更新 OAuth Provider 登录启用状态 */
export function fetchUpdateOAuthProviderEnabled(
    provider: string,
    params: Api.OAuth.ProviderEnableParam
) {
    return request.put<Api.OAuth.ProviderConfig>({
        url: `/api/oauth/providers/${provider}/enabled`,
        params
    })
}

/** 查询当前用户的 OAuth 绑定 */
export function fetchOAuthBindings() {
    return request.get<Api.OAuth.Binding[]>({
        url: '/api/oauth/bindings'
    })
}

/** 发起 OAuth 账号绑定，返回授权地址 */
export function fetchStartOAuthBind(provider: string) {
  const returnOrigin = encodeURIComponent(window.location.origin)
  return request.post<Api.OAuth.AuthorizeStart>({
    url: `/api/oauth/bindings/${provider}/start?return_origin=${returnOrigin}`
  })
}

/** 解除 OAuth 账号绑定 */
export function fetchUnbindOAuth(provider: string) {
  return request.del({
    url: `/api/oauth/bindings/${provider}`
  })
}

/** 跳转至 OAuth 授权页（登录）；携带当前前端 origin，供开发态回跳 */
export function redirectOAuthAuthorize(provider: string) {
  const returnOrigin = encodeURIComponent(window.location.origin)
  window.location.href = `/api/auth/oauth/authorize/${provider.toLowerCase()}?return_origin=${returnOrigin}`
}
