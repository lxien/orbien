<template>
  <ElFormItem label="带宽限制" prop="limitTotal">
    <div class="bandwidth-limit-field">
      <ElInputNumber
          v-model="limitTotal"
          placeholder="留空不限制"
          :controls="false"
          :min="1"
          :precision="0"
          class="bandwidth-input"
      />
      <span class="bandwidth-unit">Mbps</span>
    </div>
  </ElFormItem>
</template>

<script setup lang="ts">
import type {LimitTotalMbps} from './bandwidth-limit'

defineOptions({name: 'BandwidthLimitField'})

const limitTotal = defineModel<LimitTotalMbps>()
</script>

<style scoped lang="scss">
.bandwidth-limit-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bandwidth-input {
  width: 200px;
}

.bandwidth-unit {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
