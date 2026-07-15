import { h } from 'vue'
import { ByteUtils } from '@/utils/format/byteFormatter'

const CELL_STYLE: Record<string, string> = {
  display: 'inline-flex',
  flexDirection: 'column',
  gap: '2px',
  margin: '0',
  padding: '2px 4px',
  border: 'none',
  borderRadius: '4px',
  background: 'transparent',
  cursor: 'pointer',
  lineHeight: '1.25',
  textAlign: 'left'
}

const RATE_STYLE: Record<string, string> = {
  fontSize: '12px',
  fontVariantNumeric: 'tabular-nums',
  whiteSpace: 'nowrap'
}

export function renderTrafficRate(
  traffic: Api.Proxy.ProxyTrafficSnippet | undefined,
  onClick?: () => void
) {
  const up = `${ByteUtils.formatBytes(traffic?.upRate || 0)}/s`
  const down = `${ByteUtils.formatBytes(traffic?.downRate || 0)}/s`

  return h(
    'button',
    {
      type: 'button',
      style: CELL_STYLE,
      title: `上行 ${up} · 下行 ${down}`,
      onClick: (e: MouseEvent) => {
        e.stopPropagation()
        onClick?.()
      }
    },
    [
      h('span', { style: { ...RATE_STYLE, color: 'var(--el-color-success)' } }, `↑ ${up}`),
      h('span', { style: { ...RATE_STYLE, color: 'var(--el-color-primary)' } }, `↓ ${down}`)
    ]
  )
}
