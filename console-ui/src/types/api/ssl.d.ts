declare namespace Api.Ssl {
  interface CertDTO {
    id: string
    issuer: string
    org: string
    sanDomains: string[]
    status: 1 | 2
    source?: 1 | 2
    boundDomainCount?: number
    notBefore: string
    notAfter: string
    autoRenew?: boolean
    lastRenewAt?: string
  }

  interface CertSaveParams {
    key: string
    fullChain: string
  }

  interface CertSaveAndDeployParams {
    proxyId: string
    key: string
    fullChain: string
    proxyDomainIds?: number[]
  }

  interface AutoRenewResult {
    autoRenew: boolean
    acmeRenewJobAutoEnabled?: boolean
  }
}
