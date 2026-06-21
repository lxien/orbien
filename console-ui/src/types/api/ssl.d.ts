declare namespace Api.Ssl {
  interface CertDTO {
    id: number
    issuer: string
    issuer0: string
    subject: string
    sanDomains: string
    status: 1 | 2
    notBefore: string
    notAfter: string
  }

  interface CertSaveParams {
    key: string
    fullChain: string
  }
}
