declare namespace Api.ScheduledJob {
  interface ParamFieldDTO {
    key: string
    label: string
    type: 'number' | 'boolean' | 'string'
    required: boolean
    min?: number
    max?: number
    description?: string
  }

  interface JobDTO {
    jobCode: string
    jobName: string
    description: string
    enabled: boolean
    cronExpression: string
    params: Record<string, unknown>
    paramSchema: ParamFieldDTO[]
    lastRunAt?: string
    lastRunStatus?: number
    lastRunStatusLabel?: string
    lastRunMessage?: string
    nextRunAt?: string
  }

  interface JobUpdateParam {
    cronExpression: string
    params: Record<string, unknown>
  }

  interface JobLogDTO {
    id: number
    jobCode: string
    triggerType: number
    triggerTypeLabel: string
    startedAt: string
    finishedAt: string
    status: number
    statusLabel: string
    affectedCount: number
    message?: string
  }
}
