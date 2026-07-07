declare namespace Api.AcmeOrder {
    interface DnsChallenge {
        id: number
        domain: string
        recordName: string
        hostRecord?: string
        dnsZone?: string
        recordValue: string
        recordType?: string
        status: number
        statusLabel?: string
    }

    interface OrderDTO {
        id: number
        orderNo: string
        status: number
        statusLabel?: string
        domains: string[]
        validationMode: number
        dnsCredentialId?: number
        dnsProvider?: number
        certId?: string
        bindProxyDomainIds?: number[]
        errorCode?: string
        errorMessage?: string
        expiresAt?: string
        createdAt?: string
        challenges?: DnsChallenge[]
    }

    interface CreateParams {
        domains: string[]
        validationMode: number
        dnsCredentialId?: number
        bindProxyDomainIds?: number[]
        autoRenew?: boolean
    }

    interface HttpsProxyOption {
        proxyId: string
        name: string
        agentId: string
        agentName: string
        status: number
        domainCount: number
        domainPreview: string[]
    }

    interface HttpsProxyDomainOption {
        proxyDomainId: number
        fullDomain: string
        domainType?: number
        domainTypeLabel?: string
        selectable: boolean
        unselectableReason?: string
        bound: boolean
        boundCertIssuer?: string
    }

    type DeleteIds = number[]
}
