import { h } from 'vue'
import { ElSpace, ElTag } from 'element-plus'

import { TargetHealthStatus } from '@/enums/orbien/business'

export type TargetHealthState = 'up' | 'down' | 'unknown'

const HEALTH_DOT_COLOR: Record<TargetHealthState, string> = {
  up: 'var(--el-color-success)',
  down: 'var(--el-color-danger)',
  unknown: 'var(--el-color-info)'
}

export function resolveTargetHealthStatus(target: Api.Proxy.TargetDTO): TargetHealthState {
  if (target.healthStatus === TargetHealthStatus.UP) return 'up'
  if (target.healthStatus === TargetHealthStatus.DOWN) return 'down'
  return 'unknown'
}

export function renderTargetTag(target: Api.Proxy.TargetDTO, options?: { showHealth?: boolean }) {
  const text = `${target.host}:${target.port}`
  const showHealth = options?.showHealth ?? true
  if (!showHealth) {
    return h(ElTag, { type: 'primary', size: 'small' }, () => text)
  }
  const status = resolveTargetHealthStatus(target)
  return h(ElTag, { type: 'primary', size: 'small' }, () =>
    h('span', { style: { display: 'inline-flex', alignItems: 'center', gap: '6px' } }, [
      h('span', {
        style: {
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: HEALTH_DOT_COLOR[status],
          flexShrink: '0'
        }
      }),
      text
    ])
  )
}

export function renderTargetTags(
  targets?: Api.Proxy.TargetDTO[],
  options?: { showHealth?: boolean }
) {
  if (!targets?.length) return ''
  return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
    targets.map((target) => renderTargetTag(target, options))
  )
}
