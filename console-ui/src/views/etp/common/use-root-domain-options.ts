import { ref } from 'vue'
import { fetchGetDomainListAll } from '@/api/domain'

export function normalizeRootDomainId(value: unknown): number | undefined {
  if (value == null || value === '') {
    return undefined
  }
  const id = Number(value)
  return Number.isFinite(id) ? id : undefined
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
    if (normalizedId != null && rootDomains.value.some((item) => item.id === normalizedId)) {
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
    binding: Api.Proxy.SubdomainBindingParam
  ): Api.Proxy.SubdomainBindingParam => ({
    rootDomainId: ensureDefaultRootDomainId(binding.rootDomainId),
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
  bindings?: Api.Proxy.SubdomainBindingParam[] | null
): SubdomainBindingsValidationResult {
  if (!bindings?.length) {
    return { valid: false, message: '请至少添加一条子域名配置', errorIndexes: [] }
  }

  const emptyPrefixIndexes: number[] = []
  const missingRootDomainIndexes: number[] = []

  bindings.forEach((row, index) => {
    if (!row.prefix?.trim()) {
      emptyPrefixIndexes.push(index)
    }
    if (normalizeRootDomainId(row.rootDomainId) == null) {
      missingRootDomainIndexes.push(index)
    }
  })

  if (emptyPrefixIndexes.length > 0) {
    return { valid: false, message: '请填写子域名前缀', errorIndexes: emptyPrefixIndexes }
  }

  if (missingRootDomainIndexes.length > 0) {
    return { valid: false, message: '请选择根域名', errorIndexes: [] }
  }

  const duplicateIndexes = new Set<number>()
  const seen = new Map<string, number>()
  bindings.forEach((row, index) => {
    const rootDomainId = normalizeRootDomainId(row.rootDomainId)
    const prefix = row.prefix.trim()
    const key = `${rootDomainId}:${prefix}`
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

  return { valid: true, errorIndexes: [] }
}

export function buildSubdomainBindingsPayload(
  bindings: Api.Proxy.SubdomainBindingParam[]
): Api.Proxy.SubdomainBindingParam[] {
  return bindings.map((item) => ({
    rootDomainId: normalizeRootDomainId(item.rootDomainId)!,
    prefix: item.prefix.trim()
  }))
}
