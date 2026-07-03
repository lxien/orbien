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
}
