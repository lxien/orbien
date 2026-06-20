declare namespace Api.Ssl {
  interface CertDTO {}

  interface CertSaveParams {
    key: string
    fullChain: string
  }
}
