<template>
  <div class="acme-wizard validation-step">
    <ElRadioGroup :model-value="validationMode" class="validation-modes"
                  @update:model-value="emit('update:validationMode', $event)">
      <label class="mode-card art-card" :class="{ 'is-active': validationMode === 1 }">
        <ElRadio :value="1"/>
        <div class="mode-card__body">
          <div class="mode-card__title">手动 DNS 验证</div>
          <div class="mode-card__desc">自行在 DNS 服务商添加 TXT 记录，适合临时申请</div>
        </div>
      </label>
      <label class="mode-card art-card" :class="{ 'is-active': validationMode === 2 }">
        <ElRadio :value="2"/>
        <div class="mode-card__body">
          <div class="mode-card__title">
            云 DNS 自动解析
            <ElTag size="small" type="success" effect="plain" class="mode-card__tag">推荐</ElTag>
          </div>
          <div class="mode-card__desc">自动添加 TXT、自动验证，支持在证书列表开启自动续期</div>
        </div>
      </label>
    </ElRadioGroup>

    <div v-if="validationMode === 2" class="dns-credential-block">
      <ElAlert
          type="info"
          :closable="false"
          show-icon
          title="使用云 DNS 需先配置厂商 API 密钥，签发后可在证书列表手动开启自动续期"
          class="dns-hint"
      />

      <div v-if="credentialLoading" v-loading="true" class="credential-loading"/>

      <ElAlert v-else-if="!credentialList.length" type="warning" :closable="false" show-icon>
        <template #title>尚未配置 DNS 密钥</template>
        <div class="empty-credential">
          <span>添加密钥后即可自动完成 DNS 验证</span>
          <ElButton type="primary" size="small" @click="emit('add-credential')">添加 DNS 密钥</ElButton>
        </div>
      </ElAlert>

      <ElForm v-else label-width="88px" class="credential-form">
        <ElFormItem label="DNS 密钥" required>
          <div class="credential-row">
            <ElSelect
                :model-value="dnsCredentialId"
                filterable
                placeholder="选择用于自动解析的密钥"
                style="flex: 1"
                @update:model-value="emit('update:dnsCredentialId', $event)"
            >
              <ElOption
                  v-for="item in credentialList"
                  :key="item.id"
                  :label="`${item.name}（${item.providerLabel}）`"
                  :value="item.id"
              />
            </ElSelect>
            <ElButton link type="primary" @click="emit('add-credential')">添加</ElButton>
          </div>
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({name: 'AcmeApplyStepValidation'})

defineProps<{
  validationMode: number
  dnsCredentialId?: number
  credentialList: Api.DnsCredential.CredentialDTO[]
  credentialLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:validationMode', value: number): void
  (e: 'update:dnsCredentialId', value?: number): void
  (e: 'add-credential'): void
}>()
</script>

<style lang="scss">
@use './acme-apply-shared.scss';
</style>
