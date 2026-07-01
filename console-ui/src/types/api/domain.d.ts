declare namespace Api.Domain {
  interface DomainDTO {
    id: number
    domain: string
    createdAt: string
    updatedAt: string
  }

  interface UsedDomainDTO {
    id: number
    fullDomain: string
    domain: string
    baseDomain?: string
    domainType: number
    proxyId: string
    proxyName?: string
  }
}
