import { ref } from 'vue'
import { fetchUpdateProxyStatus } from '@/api/proxy'
import { ProxyStatus } from '@/enums/orbien/business'

interface ProxyStatusRow {
  id: string
  status: number
}

export function useProxyStatusToggle() {
  const togglingIds = ref<Set<string>>(new Set())

  const isToggling = (id: string) => togglingIds.value.has(id)

  const handleStatusChange = async (row: ProxyStatusRow, enabled: boolean) => {
    if (togglingIds.value.has(row.id)) {
      return
    }

    const previousStatus = row.status
    const nextStatus = enabled ? ProxyStatus.OPEN : ProxyStatus.CLOSED
    if (previousStatus === nextStatus) {
      return
    }

    row.status = nextStatus
    togglingIds.value.add(row.id)

    try {
      await fetchUpdateProxyStatus(row.id, { status: nextStatus })
    } catch {
      row.status = previousStatus
    } finally {
      togglingIds.value.delete(row.id)
    }
  }

  return {
    isToggling,
    handleStatusChange
  }
}
