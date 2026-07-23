<template>
  <ElDrawer
      v-model="visibleModel"
      :title="drawerTitle"
      size="82%"
      destroy-on-close
      append-to-body
      class="inspector-drawer"
  >
    <div v-loading="configLoading" class="inspector-page">
      <div class="inspector-toolbar">
        <div class="inspector-toolbar__left">
          <span class="inspector-toolbar__label">抓包</span>
          <ElSwitch
              v-model="inspectorEnabled"
              :loading="switchLoading"
              inline-prompt
              active-text="开"
              inactive-text="关"
              @change="handleToggleInspector"
          />
        </div>
        <ElButton :disabled="!records.length" @click="handleClear">清空</ElButton>
      </div>

      <div class="inspector-body">
        <div class="inspector-list">
          <div class="inspector-list__caption">最近 {{ INSPECTOR_DISPLAY_LIMIT }} 条</div>
          <div v-if="!records.length" class="inspector-empty">暂无捕获记录</div>
          <button
              v-for="item in records"
              :key="item.id"
              type="button"
              class="inspector-list-item"
              :class="{ 'is-active': selectedId === item.id, 'is-new': item.id === highlightId }"
              @click="selectRecord(item.id)"
          >
            <div class="inspector-list-item__line">
              <span v-if="item.replay" class="inspector-list-item__replay" title="重放产生">↻</span>
              <span class="inspector-list-item__method">{{ item.method || 'HTTP' }}</span>
              <span class="inspector-list-item__path">{{ item.path || '/' }}</span>
            </div>
            <div class="inspector-list-item__meta">
              <span :class="statusClass(item.status)">{{ formatStatus(item) }}</span>
              <span>{{ formatDuration(item.durationMs) }}</span>
            </div>
          </button>
        </div>

        <div v-loading="detailLoading || replayLoading" class="inspector-detail">
          <template v-if="detail">
            <div class="inspector-detail__header">
              <div class="inspector-detail__header-main">
                <div class="inspector-detail__title">
                  {{ detail.method }} {{ detail.path }}
                </div>
                <div class="inspector-detail__meta">
                  <span>{{ detail.scheme }}://{{ detail.host || '-' }}</span>
                  <span :class="statusClass(detail.status)">{{ formatStatus(detail) }}</span>
                  <span>{{ formatDuration(detail.durationMs) }}</span>
                  <span v-if="detail.clientIp">来自 {{ detail.clientIp }}</span>
                  <span v-if="detail.replay && detail.sourceRecordId" class="inspector-detail__source">
                    重放自 {{ detail.sourceRecordId }}
                  </span>
                </div>
              </div>
              <ElTooltip
                  :disabled="replayEnabled"
                  :content="replayDisabledReason"
                  placement="top"
              >
                <span class="inspector-replay-trigger">
                  <ElDropdown
                      trigger="click"
                      placement="bottom-end"
                      :disabled="!replayEnabled || replayLoading"
                      popper-class="inspector-replay-dropdown"
                      @command="handleReplayCommand"
                  >
                    <ElButton
                        type="primary"
                        plain
                        size="small"
                        :loading="replayLoading"
                        :disabled="!replayEnabled"
                    >
                      重放
                      <span class="inspector-replay-caret">▾</span>
                    </ElButton>
                    <template #dropdown>
                      <ElDropdownMenu>
                        <ElDropdownItem command="replay">重放</ElDropdownItem>
                        <ElDropdownItem command="edit">编辑重放</ElDropdownItem>
                      </ElDropdownMenu>
                    </template>
                  </ElDropdown>
                </span>
              </ElTooltip>
            </div>

            <section class="inspector-section">
              <div class="inspector-section__title">Request</div>
              <ElTabs v-model="requestTab" class="inspector-tabs">
                <ElTabPane label="Headers" name="headers">
                  <pre class="inspector-code">{{ formatHeaders(detail.requestHeaders) }}</pre>
                </ElTabPane>
                <ElTabPane label="Raw" name="raw">
                  <pre class="inspector-code">{{ detail.rawRequest || '' }}</pre>
                </ElTabPane>
                <ElTabPane label="Body" name="body">
                  <pre class="inspector-code">{{ formatPrettyBody(detail.requestBodyPreview) }}</pre>
                  <div v-if="detail.requestBodyTruncated" class="inspector-truncated">Body 已截断</div>
                </ElTabPane>
              </ElTabs>
            </section>

            <section class="inspector-section">
              <div class="inspector-section__title">
                Response
                <span :class="statusClass(detail.status)">{{ formatStatus(detail) }}</span>
              </div>
              <ElTabs v-model="responseTab" class="inspector-tabs">
                <ElTabPane label="Headers" name="headers">
                  <pre class="inspector-code">{{ formatHeaders(detail.responseHeaders) }}</pre>
                </ElTabPane>
                <ElTabPane label="Raw" name="raw">
                  <pre class="inspector-code">{{ detail.rawResponse || '' }}</pre>
                </ElTabPane>
                <ElTabPane label="Body" name="body">
                  <pre class="inspector-code">{{ formatPrettyBody(detail.responseBodyPreview) }}</pre>
                  <div v-if="detail.responseBodyTruncated" class="inspector-truncated">Body 已截断</div>
                </ElTabPane>
              </ElTabs>
            </section>
          </template>
          <div v-else class="inspector-empty inspector-empty--detail">选择左侧请求查看详情</div>
        </div>
      </div>
    </div>

    <ReplayEditor
        v-model:visible="editorVisible"
        :detail="detail"
        :submitting="replayLoading"
        @submit="handleEditReplaySubmit"
    />
  </ElDrawer>
</template>

<script setup lang="ts">
import {computed, ref, watch, onBeforeUnmount} from 'vue'
import {ElMessageBox} from 'element-plus'
import {useUserStore} from '@/store/modules/user'
import {
  buildInspectorStreamUrl,
  fetchClearInspectorRequests,
  fetchInspectorConfig,
  fetchInspectorRequestDetail,
  fetchInspectorRequests,
  fetchReplayInspectorRequest,
  fetchUpdateInspectorConfig,
  INSPECTOR_DISPLAY_LIMIT
} from '@/api/inspector'
import ReplayEditor from './components/ReplayEditor.vue'

defineOptions({name: 'InspectorDrawer'})

const ALLOWED_METHODS = new Set(['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'])

const props = defineProps<{
  visible: boolean
  proxyId: string
  proxyName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const visibleModel = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const drawerTitle = computed(() =>
    props.proxyName ? `流量 · ${props.proxyName}` : '流量'
)

const configLoading = ref(false)
const switchLoading = ref(false)
const detailLoading = ref(false)
const replayLoading = ref(false)
const editorVisible = ref(false)
const inspectorEnabled = ref(false)
const records = ref<Api.Inspector.RecordSummary[]>([])
const selectedId = ref('')
const highlightId = ref('')
const detail = ref<Api.Inspector.RecordDetail | null>(null)
const requestTab = ref('headers')
const responseTab = ref('raw')

let eventSource: EventSource | null = null
let highlightTimer: ReturnType<typeof setTimeout> | null = null
let sseReconnectTimer: ReturnType<typeof setTimeout> | null = null
let sseGeneration = 0

const replayDisabledReason = computed(() => {
  if (!inspectorEnabled.value) return '先开启抓包'
  if (!detail.value) return ''
  if (detail.value.requestBodyTruncated) return '请求体已截断'
  const method = (detail.value.method || '').toUpperCase()
  if (!method || !ALLOWED_METHODS.has(method)) return '方法无效'
  if (detail.value.status === 101) return '不支持协议升级'
  if (hasUpgradeHeader(detail.value.requestHeaders) || hasUpgradeHeader(detail.value.responseHeaders)) {
    return '不支持协议升级'
  }
  return ''
})

const replayEnabled = computed(() => !replayDisabledReason.value)

const hasUpgradeHeader = (headers?: Record<string, string>) => {
  if (!headers) return false
  return Object.keys(headers).some((key) => key.toLowerCase() === 'upgrade')
}

const formatDuration = (ms?: number) => {
  if (ms == null) return '-'
  return `${ms.toFixed(2)}ms`
}

const formatStatus = (item: { status?: number; statusText?: string }) => {
  if (!item.status) return '—'
  return item.statusText ? `${item.status} ${item.statusText}` : String(item.status)
}

const statusClass = (status?: number) => {
  if (!status) return 'status-muted'
  if (status >= 500) return 'status-5xx'
  if (status >= 400) return 'status-4xx'
  if (status >= 300) return 'status-3xx'
  return 'status-2xx'
}

const formatHeaders = (headers?: Record<string, string>) => {
  if (!headers || !Object.keys(headers).length) return ''
  return Object.entries(headers)
      .map(([key, value]) => `${key}: ${value}`)
      .join('\n')
}

const formatPrettyBody = (body?: string) => {
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
}

const loadConfig = async () => {
  if (!props.proxyId) return
  configLoading.value = true
  try {
    const config = await fetchInspectorConfig(props.proxyId)
    inspectorEnabled.value = config.inspectorEnabled
  } finally {
    configLoading.value = false
  }
}

const loadRecords = async () => {
  if (!props.proxyId) return
  records.value = (await fetchInspectorRequests(props.proxyId, INSPECTOR_DISPLAY_LIMIT)) || []
  if (selectedId.value && !records.value.some((item) => item.id === selectedId.value)) {
    selectedId.value = records.value[0]?.id || ''
  }
  if (!selectedId.value && records.value.length) {
    selectedId.value = records.value[0].id
  }
  if (selectedId.value) {
    await loadDetail(selectedId.value)
  } else {
    detail.value = null
  }
}

const loadDetail = async (id: string) => {
  detailLoading.value = true
  try {
    detail.value = await fetchInspectorRequestDetail(id)
  } finally {
    detailLoading.value = false
  }
}

const selectRecord = async (id: string) => {
  selectedId.value = id
  await loadDetail(id)
}

const handleToggleInspector = async (enabled: boolean) => {
  switchLoading.value = true
  try {
    const config = await fetchUpdateInspectorConfig({
      proxyId: props.proxyId,
      inspectorEnabled: enabled
    })
    inspectorEnabled.value = config.inspectorEnabled
  } catch {
    inspectorEnabled.value = !enabled
  } finally {
    switchLoading.value = false
  }
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定清空？', '清空', {type: 'warning'})
  await fetchClearInspectorRequests(props.proxyId)
  records.value = []
  selectedId.value = ''
  detail.value = null
}

const prependRecord = (summary: Api.Inspector.RecordSummary, select = true) => {
  if (summary.proxyId && summary.proxyId !== props.proxyId) {
    return
  }
  records.value = [summary, ...records.value.filter((item) => item.id !== summary.id)].slice(
      0,
      INSPECTOR_DISPLAY_LIMIT
  )
  highlightId.value = summary.id
  if (highlightTimer) clearTimeout(highlightTimer)
  highlightTimer = setTimeout(() => {
    highlightId.value = ''
  }, 2000)
  if (select) {
    selectedId.value = summary.id
    void loadDetail(summary.id)
  }
}

const applyReplayResult = (result: Api.Inspector.ReplayResult) => {
  const record = result.record
  if (!record) return
  prependRecord({
    id: record.id,
    proxyId: record.proxyId,
    streamId: record.streamId,
    startedAt: record.startedAt,
    durationMs: record.durationMs,
    method: record.method,
    path: record.path,
    host: record.host,
    scheme: record.scheme,
    status: record.status,
    statusText: record.statusText,
    replay: record.replay ?? true,
    sourceRecordId: record.sourceRecordId || result.sourceRecordId
  })
  detail.value = record
}

const doReplay = async (overrides?: Api.Inspector.ReplayOverrides) => {
  if (!detail.value) return
  replayLoading.value = true
  try {
    const result = await fetchReplayInspectorRequest(detail.value.id, {
      captureToBuffer: true,
      timeoutSeconds: 30,
      overrides
    })
    editorVisible.value = false
    applyReplayResult(result)
  } finally {
    replayLoading.value = false
  }
}

const handleReplayCommand = async (command: string | number | object) => {
  if (!detail.value || !replayEnabled.value) return
  if (command === 'edit') {
    editorVisible.value = true
    return
  }
  if (command === 'replay') {
    try {
      await doReplay()
    } catch {
    }
  }
}

const handleEditReplaySubmit = async (overrides: Api.Inspector.ReplayOverrides) => {
  if (!detail.value) return
  try {
    await doReplay(overrides)
  } catch {
    // cancelled / failed
  }
}

const connectSse = () => {
  disconnectSse()
  if (!props.proxyId || !props.visible) return

  const generation = sseGeneration
  const userStore = useUserStore()
  const url = buildInspectorStreamUrl(props.proxyId, userStore.accessToken || undefined)
  eventSource = new EventSource(url)

  eventSource.addEventListener('request.captured', (event) => {
    try {
      const summary = JSON.parse(event.data) as Api.Inspector.RecordSummary
      // 重放成功时已本地选中，SSE 再来只合并列表、避免抢焦点
      const alreadySelected = summary.id === selectedId.value
      prependRecord(summary, !alreadySelected && !replayLoading.value)
    } catch {
      // ignore malformed payload
    }
  })

  eventSource.addEventListener('buffer.cleared', (event) => {
    try {
      const payload = JSON.parse(event.data) as { proxyId?: string }
      if (payload.proxyId && payload.proxyId !== props.proxyId) {
        return
      }
    } catch {
      // ignore malformed payload
    }
    records.value = []
    selectedId.value = ''
    detail.value = null
  })

  eventSource.onerror = () => {
    if (generation !== sseGeneration || !props.visible) {
      return
    }
    disconnectSse(false)
    void loadRecords()
    sseReconnectTimer = setTimeout(() => {
      if (generation === sseGeneration && props.visible) {
        connectSse()
      }
    }, 2000)
  }
}

const disconnectSse = (bumpGeneration = true) => {
  if (bumpGeneration) {
    sseGeneration += 1
  }
  if (sseReconnectTimer) {
    clearTimeout(sseReconnectTimer)
    sseReconnectTimer = null
  }
  if (eventSource) {
    eventSource.onerror = null
    eventSource.close()
    eventSource = null
  }
}

watch(
    () => [props.visible, props.proxyId] as const,
    async ([visible, proxyId]) => {
      if (!visible) {
        disconnectSse()
        editorVisible.value = false
        return
      }
      if (!proxyId) return
      await Promise.all([loadConfig(), loadRecords()])
      connectSse()
    },
    {immediate: true}
)

onBeforeUnmount(() => {
  disconnectSse()
  if (highlightTimer) clearTimeout(highlightTimer)
})
</script>

<style scoped lang="scss">
.inspector-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  min-height: 520px;
}

.inspector-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.inspector-toolbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.inspector-toolbar__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.inspector-list__caption {
  padding: 10px 14px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.inspector-body {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.inspector-list {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow-y: auto;
  background: var(--el-bg-color);
}

.inspector-list-item {
  display: block;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-active {
    background: color-mix(in srgb, var(--theme-color) 10%, var(--default-box-color));
  }

  &.is-new {
    animation: inspector-flash 1.2s ease;
  }
}

.inspector-list-item__line {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  min-width: 0;
  align-items: center;
}

.inspector-list-item__replay {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--theme-color);
  font-weight: 600;
}

.inspector-list-item__method {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--theme-color);
}

.inspector-list-item__path {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.inspector-list-item__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.inspector-detail {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
  background: var(--el-bg-color);
}

.inspector-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.inspector-detail__header-main {
  min-width: 0;
  flex: 1;
}

.inspector-detail__title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  word-break: break-all;
}

.inspector-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.inspector-detail__source {
  color: var(--theme-color);
}

.inspector-replay-trigger {
  display: inline-flex;
  flex-shrink: 0;
  margin-right: 8px;
}

.inspector-replay-caret {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.85;
}

.inspector-section + .inspector-section {
  margin-top: 20px;
}

.inspector-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
}

.inspector-code {
  margin: 0;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 280px;
  overflow: auto;
}

.inspector-truncated {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-color-warning);
}

.inspector-empty {
  padding: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;

  &--detail {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.status-2xx {
  color: var(--el-color-primary);
}

.status-3xx {
  color: var(--el-text-color-secondary);
}

.status-4xx {
  color: var(--el-color-warning);
}

.status-5xx {
  color: var(--el-color-danger);
}

.status-muted {
  color: var(--el-text-color-placeholder);
}

@keyframes inspector-flash {
  0% {
    background: color-mix(in srgb, var(--theme-color) 24%, transparent);
  }
  100% {
    background: transparent;
  }
}
</style>

<style lang="scss">
.inspector-replay-dropdown {
  min-width: 140px;

  .el-dropdown-menu__item {
    padding: 8px 16px;
    white-space: nowrap;
  }
}
</style>
