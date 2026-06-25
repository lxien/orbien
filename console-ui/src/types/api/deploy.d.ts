declare namespace Api.Deploy {
  interface SslDeployInfoDTO {
    deployId: number
    certId: string
    proxyId: string
    issuer: string
    org: string
    sanDomains: string[]
    notBefore: string
    notAfter: string
    keyPem: string
    fullChainPem: string
    enabled: boolean
  }

  interface SslDeployDTO {
    domains: string[]
  }

  interface SslDeployParams {
    certId: string
    proxyIds: string[]
  }
}
