<template>
  <ElDialog
      :model-value="visible"
      title="编辑重放"
      width="70%"
      append-to-body
      destroy-on-close
      class="replay-editor-dialog"
      @close="emit('update:visible', false)"
  >
    <div v-if="detail" class="replay-editor">
      <div class="replay-editor__meta">
        <span>源请求 {{ detail.method }} {{ detail.path }}</span>
        <span class="replay-editor__meta-muted">· {{ detail.id }}</span>
      </div>

      <div class="replay-editor__row">
        <div class="replay-editor__field replay-editor__field--method">
          <label>Method</label>
          <ElSelect v-model="form.method" style="width: 100%">
            <ElOption v-for="m in METHODS" :key="m" :label="m" :value="m"/>
          </ElSelect>
        </div>
        <div class="replay-editor__field replay-editor__field--path">
          <label>Path</label>
          <ElInput v-model="form.path" placeholder="/path?query=1"/>
        </div>
      </div>

      <section class="replay-fold" :class="{ 'is-open': headersOpen }">
        <div class="replay-fold__bar">
          <button type="button" class="replay-fold__toggle" @click="headersOpen = !headersOpen">
            <span class="replay-fold__chevron" aria-hidden="true">›</span>
            <span class="replay-fold__title">Headers</span>
            <span class="replay-fold__count">{{ editableHeaders.length }}</span>
            <span v-if="!headersOpen && headerPreview" class="replay-fold__preview">{{ headerPreview }}</span>
          </button>
          <ElButton text type="primary" @click.stop="onAddHeader">+ 添加</ElButton>
        </div>
        <div v-show="headersOpen" class="replay-fold__body">
          <div v-if="!editableHeaders.length" class="replay-editor__empty">暂无</div>
          <div
              v-for="(row, index) in editableHeaders"
              :key="index"
              class="replay-editor__header-row"
          >
            <ElInput
                v-model="row.name"
                placeholder="Name"
                :class="{ 'is-error': row.error }"
                @blur="validateHeaderRow(row)"
            />
            <ElInput v-model="row.value" placeholder="Value"/>
            <ElButton text type="danger" @click="removeHeader(index)">✕</ElButton>
          </div>
          <div v-if="headerError" class="replay-editor__error">{{ headerError }}</div>
        </div>
      </section>

      <section class="replay-fold" :class="{ 'is-open': systemOpen }">
        <div class="replay-fold__bar">
          <button type="button" class="replay-fold__toggle" @click="systemOpen = !systemOpen">
            <span class="replay-fold__chevron" aria-hidden="true">›</span>
            <span class="replay-fold__title">系统 Header</span>
            <span class="replay-fold__count">{{ systemHeaders.length }}</span>
          </button>
        </div>
        <div v-show="systemOpen" class="replay-fold__body">
          <div class="replay-editor__system">
            <div v-for="item in systemHeaders" :key="item.name" class="replay-editor__system-item">
              <span class="replay-editor__system-name">{{ item.name }}</span>
              <span class="replay-editor__system-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </section>

      <div class="replay-editor__section">
        <div class="replay-editor__section-head">
          <span>Body</span>
          <div class="replay-editor__section-actions">
            <span class="replay-editor__hint">{{ bodySize }} / {{ maxBodyBytes }}</span>
            <ElButton text type="primary" :disabled="!canFormatBody" @click="formatBody">
              格式化
            </ElButton>
          </div>
        </div>
        <ElInput
            v-model="form.body"
            type="textarea"
            :rows="10"
            resize="vertical"
            class="replay-editor__body"
            placeholder=""
        />
        <div v-if="bodyTooLarge" class="replay-editor__error">超出大小限制</div>
      </div>
    </div>

    <template #footer>
      <ElButton type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
        发送重放
      </ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue'

defineOptions({name: 'ReplayEditor'})

const METHODS = ['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'] as const

const FORBIDDEN_HEADERS = new Set([
  'connection', 'keep-alive', 'proxy-connection', 'transfer-encoding', 'te', 'trailer',
  'upgrade', 'content-length', 'x-forwarded-for', 'x-forwarded-proto', 'x-forwarded-host',
  'x-forwarded-port', 'forwarded', 'host'
])

const props = defineProps<{
  visible: boolean
  detail: Api.Inspector.RecordDetail | null
  maxBodyBytes?: number
  submitting?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'submit', overrides: Api.Inspector.ReplayOverrides): void
}>()

const maxBodyBytes = computed(() => props.maxBodyBytes ?? 65536)

const form = reactive({
  method: 'GET',
  path: '/',
  body: ''
})

interface HeaderRow {
  name: string
  value: string
  error?: string
}

const editableHeaders = ref<HeaderRow[]>([])
const systemHeaders = ref<{ name: string; value: string }[]>([])
/** 可编辑 Headers：默认展开，便于改鉴权等 */
const headersOpen = ref(true)
/** 系统 Header：默认收起，减少干扰 */
const systemOpen = ref(false)

const bodySize = computed(() => new TextEncoder().encode(form.body || '').length)
const bodyTooLarge = computed(() => bodySize.value > maxBodyBytes.value)

const canFormatBody = computed(() => {
  const raw = form.body?.trim()
  if (!raw) return false
  try {
    JSON.parse(raw)
    return true
  } catch {
    return false
  }
})

const headerPreview = computed(() => {
  const names = editableHeaders.value
      .map((row) => row.name?.trim())
      .filter(Boolean)
      .slice(0, 3)
  if (!names.length) return '未设置'
  const more = editableHeaders.value.length > names.length
  return names.join(', ') + (more ? '…' : '')
})

const headerError = computed(() => {
  for (const row of editableHeaders.value) {
    if (row.error) return row.error
    if (row.name && isForbidden(row.name)) {
      return `不可修改: ${row.name}`
    }
  }
  return ''
})

const canSubmit = computed(() => {
  if (!form.method || !METHODS.includes(form.method as typeof METHODS[number])) return false
  if (!form.path?.trim()) return false
  if (bodyTooLarge.value) return false
  if (headerError.value) return false
  return true
})

watch(
    () => [props.visible, props.detail] as const,
    ([visible, detail]) => {
      if (!visible || !detail) return
      form.method = (detail.method || 'GET').toUpperCase()
      form.path = detail.path || '/'
      form.body = prettyBody(detail.requestBodyPreview)
      const editable: HeaderRow[] = []
      const system: { name: string; value: string }[] = []
      const headers = detail.requestHeaders || {}
      for (const [name, value] of Object.entries(headers)) {
        if (isForbidden(name) || name.toLowerCase().startsWith('orbien-replay-')) {
          system.push({name, value})
        } else {
          editable.push({name, value})
        }
      }
      if (!system.some((h) => h.name.toLowerCase() === 'host') && detail.host) {
        system.unshift({name: 'Host', value: detail.host})
      }
      system.push(
          {name: 'Content-Length', value: '(自动计算)'},
          {name: 'Connection / X-Forwarded-* / Orbien-Replay-*', value: '(系统写入)'}
      )
      editableHeaders.value = editable
      systemHeaders.value = system
      // Header 较多时默认收起，把首屏留给 Method / Path / Body
      headersOpen.value = editable.length <= 6
      systemOpen.value = false
    },
    {immediate: true}
)

const isForbidden = (name: string) => {
  const lower = name.trim().toLowerCase()
  return FORBIDDEN_HEADERS.has(lower) || lower.startsWith('orbien-replay-') || lower.startsWith(':')
}

const prettyBody = (body?: string) => {
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
}

const formatBody = () => {
  if (!canFormatBody.value) return
  form.body = JSON.stringify(JSON.parse(form.body.trim()), null, 2)
}

const onAddHeader = () => {
  headersOpen.value = true
  editableHeaders.value.push({name: '', value: ''})
}

const removeHeader = (index: number) => {
  editableHeaders.value.splice(index, 1)
}

const validateHeaderRow = (row: HeaderRow) => {
  if (!row.name?.trim()) {
    row.error = undefined
    return
  }
  if (isForbidden(row.name)) {
    row.error = '不可修改'
    return
  }
  row.error = undefined
}

const handleSubmit = () => {
  if (!canSubmit.value) return
  const headers: Record<string, string> = {}
  for (const row of editableHeaders.value) {
    const name = row.name?.trim()
    if (!name) continue
    headers[name] = row.value ?? ''
  }
  emit('submit', {
    method: form.method,
    path: form.path.trim(),
    headers,
    body: form.body
  })
}
</script>

<style scoped lang="scss">
.replay-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.replay-editor__meta {
  font-size: 13px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.replay-editor__meta-muted {
  color: var(--el-text-color-secondary);
  margin-left: 4px;
}

.replay-editor__row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 12px;
}

.replay-editor__field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
  }
}

.replay-fold {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  overflow: hidden;
}

.replay-fold__bar {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 40px;
  padding: 4px 8px 4px 4px;
  background: var(--el-fill-color-lighter);
}

.replay-fold__toggle {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: var(--el-text-color-primary);

  &:hover {
    background: var(--el-fill-color);
  }
}

.replay-fold__chevron {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  transform: rotate(0deg);
  transition: transform 0.15s ease;
}

.replay-fold.is-open .replay-fold__chevron {
  transform: rotate(90deg);
}

.replay-fold__title {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 650;
}

.replay-fold__count {
  flex-shrink: 0;
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
}

.replay-fold__preview {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.replay-fold__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.replay-editor__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.replay-editor__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.replay-editor__section-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.replay-editor__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.replay-editor__header-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr) 36px;
  gap: 8px;
  align-items: center;
}

.replay-editor__header-row :deep(.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
}

.replay-editor__system {
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.replay-editor__system-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
  gap: 16px;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.replay-editor__system-name {
  color: var(--el-text-color-secondary);
}

.replay-editor__system-value {
  color: var(--el-text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.replay-editor__body :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.replay-editor__error {
  font-size: 12px;
  color: var(--el-color-danger);
}

.replay-editor__empty {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 4px 0;
}
</style>
