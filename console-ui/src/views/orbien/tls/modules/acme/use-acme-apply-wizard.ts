import {computed, ref, watch, type Ref} from 'vue'
import {ElMessage} from 'element-plus'
import {fetchDnsCredentialList} from '@/api/dns-credential'
import {
    fetchAcmeOrderDetail,
    fetchCreateAcmeOrder,
    fetchVerifyAcmeOrder
} from '@/api/acme-order'
import type {CertBrand} from './types'
import {certBrandOptions} from './types'

export function useAcmeApplyWizard(options: {
    visible: Ref<boolean>
    onSuccess: () => void
}) {
    const currentStep = ref(0)
    const certBrand = ref<CertBrand>('lets_encrypt')
    const selectedDomains = ref<Api.AcmeOrder.HttpsProxyDomainOption[]>([])
    const extraDomainText = ref('')
    const validationMode = ref(1)
    const dnsCredentialId = ref<number>()
    const credentialList = ref<Api.DnsCredential.CredentialDTO[]>([])
    const credentialLoading = ref(false)
    const dnsDialogVisible = ref(false)
    const submitting = ref(false)
    const verifying = ref(false)
    const refreshing = ref(false)
    const orderResult = ref<Api.AcmeOrder.OrderDTO | null>(null)

    const parsedExtraDomains = computed(() =>
        extraDomainText.value
            .split(/[\n,;]+/)
            .map((item) => item.trim())
            .filter(Boolean)
    )

    const canManualVerify = computed(
        () => orderResult.value && [1, 2, 6].includes(orderResult.value.status)
    )

    const buildApplyDomains = () => {
        const domains = [
            ...selectedDomains.value.map((item) => item.fullDomain),
            ...parsedExtraDomains.value
        ]
        return [...new Set(domains)]
    }

    const resetWizard = () => {
        currentStep.value = 0
        certBrand.value = 'lets_encrypt'
        selectedDomains.value = []
        extraDomainText.value = ''
        validationMode.value = 1
        dnsCredentialId.value = undefined
        dnsDialogVisible.value = false
        orderResult.value = null
    }

    const loadCredentials = async () => {
        credentialLoading.value = true
        try {
            credentialList.value = await fetchDnsCredentialList()
        } finally {
            credentialLoading.value = false
        }
    }

    const openDnsDialog = () => {
        dnsDialogVisible.value = true
    }

    const handleDnsCredentialSaved = async (saved?: Api.DnsCredential.CredentialDTO) => {
        await loadCredentials()
        if (saved?.id) {
            dnsCredentialId.value = saved.id
            return
        }
        if (credentialList.value.length === 1) {
            dnsCredentialId.value = credentialList.value[0].id
        }
    }

    const validateDomainStep = () => {
        const domains = buildApplyDomains()
        if (!domains.length) {
            ElMessage.warning('请至少选择一个域名')
            return false
        }
        return true
    }

    const goNextFromDomain = () => {
        if (!validateDomainStep()) {
            return
        }
        currentStep.value++
    }

    const handleSubmit = async () => {
        const domains = buildApplyDomains()
        if (!domains.length) {
            ElMessage.warning('请至少选择一个域名')
            return
        }
        if (validationMode.value === 2 && !dnsCredentialId.value) {
            if (!credentialList.value.length) {
                ElMessage.warning('请先添加 DNS 密钥')
                openDnsDialog()
            } else {
                ElMessage.warning('请选择 DNS 密钥')
            }
            return
        }
        submitting.value = true
        try {
            orderResult.value = await fetchCreateAcmeOrder({
                domains,
                validationMode: validationMode.value,
                dnsCredentialId: dnsCredentialId.value,
                bindProxyDomainIds: selectedDomains.value.map((item) => item.proxyDomainId)
            })
            currentStep.value = 2
            if (orderResult.value.status === 5) {
                ElMessage.success('证书申请成功')
                options.onSuccess()
            } else if (validationMode.value === 2) {
                ElMessage.info('已提交，系统正在自动验证')
            }
        } finally {
            submitting.value = false
        }
    }

    const refreshOrder = async () => {
        if (!orderResult.value?.id) return
        refreshing.value = true
        try {
            orderResult.value = await fetchAcmeOrderDetail(orderResult.value.id)
            if (orderResult.value.status === 5) {
                ElMessage.success('证书申请成功')
                options.onSuccess()
            }
        } finally {
            refreshing.value = false
        }
    }

    const handleVerify = async () => {
        if (!orderResult.value?.id) return
        verifying.value = true
        try {
            await fetchVerifyAcmeOrder(orderResult.value.id)
            ElMessage.success('已开始验证，请稍后刷新状态')
            await refreshOrder()
        } finally {
            verifying.value = false
        }
    }

    const copyText = async (text: string) => {
        await navigator.clipboard.writeText(text)
        ElMessage.success('已复制')
    }

    const initWhenVisible = async () => {
        resetWizard()
        await loadCredentials()
    }

    watch(
        () => options.visible.value,
        (visible) => {
            if (visible) {
                void initWhenVisible()
            }
        }
    )

    watch(validationMode, (mode) => {
        if (mode === 2 && !dnsCredentialId.value && credentialList.value.length === 1) {
            dnsCredentialId.value = credentialList.value[0].id
        }
    })

    return {
        certBrandOptions,
        currentStep,
        certBrand,
        selectedDomains,
        extraDomainText,
        parsedExtraDomains,
        validationMode,
        dnsCredentialId,
        credentialList,
        credentialLoading,
        dnsDialogVisible,
        submitting,
        verifying,
        refreshing,
        orderResult,
        canManualVerify,
        buildApplyDomains,
        resetWizard,
        loadCredentials,
        openDnsDialog,
        handleDnsCredentialSaved,
        validateDomainStep,
        goNextFromDomain,
        handleSubmit,
        refreshOrder,
        handleVerify,
        copyText,
        initWhenVisible
    }
}
