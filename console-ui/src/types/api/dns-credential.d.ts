declare namespace Api.DnsCredential {
  interface CredentialDTO {
    id: number
    name: string
    provider: number
    providerLabel?: string
    status: number
    accountHint?: string
    lastTestAt?: string
    lastTestMessage?: string
    createdAt?: string
  }

  interface ProviderField {
    key: string
    label: string
    required: boolean
    secret: boolean
  }

  interface ProviderSchema {
    provider: number
    label: string
    fields: ProviderField[]
  }

  interface SaveParams {
    id?: number
    name: string
    provider: number
    config: Record<string, string>
  }
}
