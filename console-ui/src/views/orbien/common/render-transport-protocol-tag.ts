import { h } from 'vue'
import { ElTag } from 'element-plus'

import { getTransportProtocolTag } from '@/enums/orbien/business'

export function renderTransportProtocolTag(transportProtocol?: number) {
  const { type, text } = getTransportProtocolTag(transportProtocol)
  return h(ElTag, { type, size: 'small' }, () => text)
}
