import {ref} from 'vue'
import {fetchGetDomainListAll} from '@/api/domain'

type SubdomainBindingLike =
    | Api.Proxy.SubdomainBindingParam
    | Api.Proxy.SubdomainBindingDTO

export function normalizeRootDomainId(value: unknown): number | undefined {
    if (value == null || value === '') {
        return undefined
    }
    const id = Number(value)
    return Number.isFinite(id) ? id : undefined
}

export function resolveSubdomainBindingRootDomainId(
    binding: SubdomainBindingLike,
    rootDomains?: Api.Domain.DomainDTO[]
): number | undefined {
    const normalizedId = normalizeRootDomainId(binding.rootDomainId)
    if (normalizedId != null && rootDomains?.some((item) => item.id == normalizedId)) {
        return normalizedId
    }

    const rootDomainName =
        'rootDomain' in binding ? binding.rootDomain?.trim().toLowerCase() : undefined
    if (rootDomainName && rootDomains?.length) {
        const matched = rootDomains.find((item) => item.domain.trim().toLowerCase() === rootDomainName)
        if (matched) {
            return normalizeRootDomainId(matched.id)
        }
    }

    return normalizedId
}

export function resolveSubdomainBindingRootDomainName(
    binding: SubdomainBindingLike,
    rootDomains?: Api.Domain.DomainDTO[]
): string | undefined {
    const rootDomainId = resolveSubdomainBindingRootDomainId(binding, rootDomains)
    if (rootDomainId != null && rootDomains?.length) {
        const matched = rootDomains.find((item) => item.id == rootDomainId)
        if (matched?.domain?.trim()) {
            return matched.domain.trim().toLowerCase()
        }
    }

    const rootDomainName =
        'rootDomain' in binding ? binding.rootDomain?.trim().toLowerCase() : undefined
    return rootDomainName || undefined
}

export function buildSubdomainBindingUniquenessKey(
    binding: SubdomainBindingLike,
    rootDomains?: Api.Domain.DomainDTO[]
): string | null {
    const prefix = binding.prefix?.trim().toLowerCase()
    if (!prefix) {
        return null
    }

    const rootDomainName = resolveSubdomainBindingRootDomainName(binding, rootDomains)
    if (!rootDomainName) {
        return null
    }

    return `${rootDomainName}:${prefix}`
}

export function useRootDomainOptions() {
    const rootDomains = ref<Api.Domain.DomainDTO[]>([])
    const rootDomainLoading = ref(false)

    const loadRootDomains = async () => {
        rootDomainLoading.value = true
        try {
            rootDomains.value = (await fetchGetDomainListAll()) || []
        } catch (error) {
            console.error('获取根域名列表失败:', error)
            rootDomains.value = []
        } finally {
            rootDomainLoading.value = false
        }
    }

    const ensureDefaultRootDomainId = (currentId?: number | null): number | undefined => {
        const normalizedId = normalizeRootDomainId(currentId)
        if (normalizedId != null && rootDomains.value.some((item) => item.id == normalizedId)) {
            return normalizedId
        }
        return normalizeRootDomainId(rootDomains.value[0]?.id)
    }

    const createDefaultSubdomainBinding = (
        currentId?: number | null
    ): Api.Proxy.SubdomainBindingParam => ({
        rootDomainId: ensureDefaultRootDomainId(currentId),
        prefix: ''
    })

    const normalizeSubdomainBinding = (
        binding: SubdomainBindingLike
    ): Api.Proxy.SubdomainBindingParam => ({
        rootDomainId:
            resolveSubdomainBindingRootDomainId(binding, rootDomains.value) ??
            ensureDefaultRootDomainId(binding.rootDomainId),
        prefix: binding.prefix?.trim() ?? ''
    })

    const normalizeSubdomainBindings = (
        bindings?: Api.Proxy.SubdomainBindingDTO[] | Api.Proxy.SubdomainBindingParam[] | null
    ): Api.Proxy.SubdomainBindingParam[] => {
        if (!bindings?.length) {
            return [createDefaultSubdomainBinding()]
        }
        return bindings.map((item) => normalizeSubdomainBinding(item))
    }

    return {
        rootDomains,
        rootDomainLoading,
        loadRootDomains,
        ensureDefaultRootDomainId,
        createDefaultSubdomainBinding,
        normalizeSubdomainBinding,
        normalizeSubdomainBindings
    }
}

export interface SubdomainBindingsValidationResult {
    valid: boolean
    message?: string
    errorIndexes: number[]
}

export function validateSubdomainBindings(
    bindings?: Api.Proxy.SubdomainBindingParam[] | null,
    rootDomains?: Api.Domain.DomainDTO[]
): SubdomainBindingsValidationResult {
    if (!bindings?.length) {
        return {valid: false, message: '请至少添加一条子域名配置', errorIndexes: []}
    }

    const emptyPrefixIndexes: number[] = []
    const missingRootDomainIndexes: number[] = []

    bindings.forEach((row, index) => {
        if (!row.prefix?.trim()) {
            emptyPrefixIndexes.push(index)
        }
        if (!resolveSubdomainBindingRootDomainName(row, rootDomains)) {
            missingRootDomainIndexes.push(index)
        }
    })

    if (emptyPrefixIndexes.length > 0) {
        return {valid: false, message: '请填写子域名前缀', errorIndexes: emptyPrefixIndexes}
    }

    if (missingRootDomainIndexes.length > 0) {
        return {valid: false, message: '请选择根域名', errorIndexes: missingRootDomainIndexes}
    }

    const duplicateIndexes = new Set<number>()
    const seen = new Map<string, number>()
    bindings.forEach((row, index) => {
        const key = buildSubdomainBindingUniquenessKey(row, rootDomains)
        if (!key) {
            return
        }
        const firstIndex = seen.get(key)
        if (firstIndex != null) {
            duplicateIndexes.add(firstIndex)
            duplicateIndexes.add(index)
        } else {
            seen.set(key, index)
        }
    })

    if (duplicateIndexes.size > 0) {
        return {
            valid: false,
            message: '存在重复的子域名配置',
            errorIndexes: [...duplicateIndexes]
        }
    }

    return {valid: true, errorIndexes: []}
}

export function buildSubdomainBindingsPayload(
    bindings: Api.Proxy.SubdomainBindingParam[],
    rootDomains?: Api.Domain.DomainDTO[]
): Api.Proxy.SubdomainBindingParam[] {
    return bindings.map((item) => ({
        rootDomainId: resolveSubdomainBindingRootDomainId(item, rootDomains)!,
        prefix: item.prefix.trim()
    }))
}
