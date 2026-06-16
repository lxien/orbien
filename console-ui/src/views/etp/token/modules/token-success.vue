<template>
  <ElDialog
    :model-value="visible"
    @update:model-value="handleClose"
    title="保存您的新令牌!"
    width="500px"
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="token-success-content">
      <p class="success-desc"
        >您的新身份验证令牌已创建！这是最后一次显示该令牌。请将其安全保存到您的计算机中。</p
      >
      <div class="token-display">
        <ElInput :model-value="token" readonly class="token-input" placeholder="Token">
          <template #suffix>
            <button class="copy-btn" @click="handleCopyOnly">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
                <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
              </svg>
            </button>
          </template>
        </ElInput>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <ElButton type="primary" @click="handleCopyAndClose" :loading="copyAndCloseLoading">
          复制并关闭
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { ElMessage } from 'element-plus'

  interface Props {
    visible: boolean
    token: string
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'close'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const copyAndCloseLoading = ref(false)

  const doCopy = async () => {
    if (!props.token) return
    try {
      await navigator.clipboard.writeText(props.token)
      ElMessage.success('复制成功')
    } catch {
      ElMessage.error('复制失败，请手动复制')
    }
  }

  const handleCopyOnly = async () => {
    await doCopy()
  }

  const handleCopyAndClose = async () => {
    copyAndCloseLoading.value = true
    try {
      await doCopy()
      emit('update:visible', false)
      emit('close')
    } finally {
      copyAndCloseLoading.value = false
    }
  }

  const handleClose = () => {
    emit('update:visible', false)
    emit('close')
  }
</script>

<style lang="scss" scoped>
  .token-success-content {
    padding: 0 16px;
  }

  .token-input {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 13px;
    letter-spacing: 1px;
  }

  .success-desc {
    font-size: 16px;
  }

  .copy-btn {
    background: none;
    border: none;
    padding: 8px;
    cursor: pointer;
    color: #909399;
    transition: color 0.2s;

    &:hover {
      color: #409eff;
    }

    &:active {
      color: #67c23a;
    }
  }
</style>
