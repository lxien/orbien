export namespace Api.Deploy {
  export interface SslDeployInfoDTO {
    deployId: number
    certId: number
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
}