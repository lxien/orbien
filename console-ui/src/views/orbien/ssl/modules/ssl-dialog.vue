<template>
  <ElDialog v-model="dialogVisible" title="上传证书" width="60%" align-center>
    <div class="cert-form">
      <div class="form-item">
        <div class="form-label">私钥(KEY)</div>
        <ElInput v-model="formData.keyContent" type="textarea" resize="none" />
      </div>

      <div class="form-item">
        <div class="form-label">证书(PEM格式)</div>
        <ElInput v-model="formData.certContent" type="textarea" resize="none" />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="handleCancel">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { reactive, computed } from 'vue'
  import { ElMessage } from 'element-plus'
  import { fetchSaveCert } from '@/api/ssl'

  defineOptions({ name: 'SslDialog' })

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

  const formData = reactive({
    keyContent: '',
    certContent: ''
  })

  const handleCancel = () => {
    dialogVisible.value = false
    formData.keyContent = ''
    formData.certContent = ''
  }

  const handleSubmit = async () => {
    if (!formData.keyContent.trim()) {
      ElMessage.warning('请输入私钥内容')
      return
    }
    if (!formData.certContent.trim()) {
      ElMessage.warning('请输入证书内容')
      return
    }

    const result = await fetchSaveCert({
      key: formData.keyContent.trim(),
      fullChain: formData.certContent.trim()
    })
    if (result) {
      ElMessage.success('证书上传成功')
      dialogVisible.value = false
      formData.keyContent = ''
      formData.certContent = ''
      emit('submit')
    }
  }
</script>

<style scoped>
  .cert-form {
    display: flex;
    gap: 30px;
    padding: 0 15px;
    min-height: 460px;
    box-sizing: border-box;
  }

  .form-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .form-label {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 8px;
    flex-shrink: 0;
  }

  :deep(.el-textarea) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-textarea__inner) {
    height: 100% !important;
  }
</style>
