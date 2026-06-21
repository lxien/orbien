<template>
  <ElDialog v-model="dialogVisible" title="部署证书" width="36%" align-center>
    <div class="deploy-content">
    </div>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="handleCancel">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">部署证书</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  defineOptions({ name: 'DeployDialog' })
  interface Props {
    visible: boolean
  }
  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }
  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()
  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })
  const handleCancel = () => {
    dialogVisible.value = false
  }
  const handleSubmit = () => {
    dialogVisible.value = false
    emit('submit')
  }
</script>

<style scoped>
  .deploy-content {
    min-height: 200px;
  }
</style>