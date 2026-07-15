declare namespace Api.Domain {
  interface DomainDTO {
    id: number
    domain: string
    remark?: string
    createdAt: string
    updatedAt: string
  }

  interface DomainCreateParam {
    domain: string
    remark?: string
  }

  interface DomainUpdateParam {
    id: number
    remark?: string
  }

  interface UsedDomainDTO {
    id: number
    fullDomain: string
    domain: string
    rootDomain?: string
    domainType: number
    proxyId: string
    proxyName?: string
    protocol?: number | null
  }
}
