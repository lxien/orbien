declare namespace Api.OAuth {
  interface PublicProvider {
    provider: string
    displayName: string
  }

  interface ProviderConfig {
    provider: string
    displayName: string
    enabled: boolean
    clientId: string
    secretConfigured: boolean
    callbackUrl: string
  }

  interface ProviderSaveParam {
    clientId?: string
    clientSecret?: string
  }

  interface ProviderEnableParam {
    enabled: boolean
  }

  interface Binding {
    provider: string
    displayName: string
    bound: boolean
    externalLogin?: string
    boundAt?: string
  }

  interface AuthorizeStart {
    authorizeUrl: string
  }
}
