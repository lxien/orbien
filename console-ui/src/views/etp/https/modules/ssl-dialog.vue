<template>
  <ElDialog
    v-model="dialogVisible"
    title="SSL 配置"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="ssl-dialog">
      <p>SSL 配置内容待实现</p>
    </div>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'

  defineOptions({ name: 'SslDialog' })

  const props = defineProps({
    visible: {
      type: Boolean,
      default: false
    },
    proxyId: {
      type: String,
      required: true
    }
  })

  const emit = defineEmits(['update:visible', 'close'])

  const dialogVisible = ref(false)

  watch(
    () => props.visible,
    (newVal) => {
      dialogVisible.value = newVal
    }
  )

  watch(dialogVisible, (newVal) => {
    emit('update:visible', newVal)
  })

  const handleClose = () => {
    dialogVisible.value = false
    emit('close')
  }
</script>
