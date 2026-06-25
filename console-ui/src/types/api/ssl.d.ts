declare namespace Api.Ssl {
  interface CertDTO {
    id: number
    issuer: string
    org: string
    sanDomains: string[]
    status: 1 | 2
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
  }
}
