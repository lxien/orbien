import type {Ref} from 'vue'

export const certBrandOptions = [{value: 'lets_encrypt', label: "Let's Encrypt"}] as const

export type CertBrand = (typeof certBrandOptions)[number]['value']

export interface AcmeWizardDomainState {
    selectedDomains: Ref<Api.AcmeOrder.HttpsProxyDomainOption[]>
    extraDomainText: Ref<string>
    certBrand: Ref<CertBrand>
}
