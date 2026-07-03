/**
 * 状态标签颜色规范
 *
 * - 二元执行结果（计划任务等）：成功=主色，失败=红色，其余=中性
 * - ACME 证书订单：按流程阶段区分颜色，失败固定红色
 */
export type OutcomeTagType = 'primary' | 'info' | 'danger'

export type AcmeOrderStatusTagType = OutcomeTagType | 'warning'

/** 计划任务执行结果：1=成功，2=失败 */
export function resolveBinaryOutcomeTagType(status?: number): OutcomeTagType {
  if (status === 1) return 'primary'
  if (status === 2) return 'danger'
  return 'info'
}

/**
 * ACME 证书申请订单
 * 0=草稿 1=待配置DNS 2=等待DNS 3=验证中 4=签发中 5=已完成 6=失败 7=已取消
 */
export function resolveAcmeOrderStatusTagType(status?: number): AcmeOrderStatusTagType {
  if (status === 5) return 'primary'
  if (status === 6) return 'danger'
  if (status === 7) return 'info'
  if (status === 3 || status === 4) return 'warning'
  if (status === 1 || status === 2) return 'warning'
  return 'info'
}

/** DNS 密钥：1=正常，2=无效 */
export function resolveDnsCredentialStatusTagType(status: number): OutcomeTagType {
  if (status === 1) return 'primary'
  if (status === 2) return 'danger'
  return 'info'
}

/** 证书绑定状态：1=正常，2=已禁用，其余异常为失败 */
export function resolveSslBindStatusTagType(status?: number): OutcomeTagType {
  if (!status) return 'info'
  if (status === 1) return 'primary'
  if (status === 2) return 'danger'
  return 'danger'
}

/** 匹配类结果 */
export function resolveBooleanMatchTagType(matched: boolean): OutcomeTagType {
  return matched ? 'primary' : 'danger'
}

/** 访问控制：放行/禁止 */
export function resolveAccessRuleTagType(isAllow: boolean): OutcomeTagType {
  return isAllow ? 'primary' : 'danger'
}

/** ACME DNS 挑战：已验证=主色，待验证=进行中 */
export function resolveAcmeChallengeTagType(verified: boolean): AcmeOrderStatusTagType {
  return verified ? 'primary' : 'warning'
}
