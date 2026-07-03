declare namespace Api.CertBinding {
  interface BindParams {
    certId: string
    proxyDomainIds: number[]
    override?: boolean
  }

  interface PreviewParams {
    certId: string
    proxyDomainIds: number[]
  }

  interface BindItemResult {
    proxyDomainId: number
    fullDomain: string
    bindingId?: number
    status: string
    reason?: string
  }

  interface BindResult {
    successCount: number
    failedCount: number
    results: BindItemResult[]
  }

  interface PreviewItem {
    proxyDomainId: number
    fullDomain: string
    proxyId?: string
    proxyName?: string
    matched: boolean
    reason?: string
    hasBinding?: boolean
    currentCertId?: string
  }

  interface DomainCertBinding {
    bindingId: number
    certId: string
    certSanDomains?: string[]
    issuer?: string
    org?: string
    notAfter?: string
    bindStatus: number
    enabled: boolean
  }

  interface ProxyDomainCertItem {
    proxyDomainId: number
    fullDomain: string
    domainType?: number
    binding?: DomainCertBinding | null
  }

  interface ProxyCertMatrix {
    proxyId: string
    totalDomains: number
    boundCount: number
    unboundCount: number
    warningCount: number
    domains: ProxyDomainCertItem[]
  }

  interface CertUsageDomain {
    bindingId: number
    proxyDomainId: number
    fullDomain: string
    proxyId?: string
    bindStatus: number
    enabled: boolean
  }

  interface CertUsage {
    certId: string
    usageCount: number
    domains: CertUsageDomain[]
  }
}
