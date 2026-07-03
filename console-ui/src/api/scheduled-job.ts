import request from '@/utils/http'

export function fetchScheduledJobList() {
  return request.get<Api.ScheduledJob.JobDTO[]>({
    url: '/api/scheduled-jobs'
  })
}

export function fetchScheduledJobDetail(jobCode: string) {
  return request.get<Api.ScheduledJob.JobDTO>({
    url: `/api/scheduled-jobs/${jobCode}`
  })
}

export function fetchUpdateScheduledJob(jobCode: string, data: Api.ScheduledJob.JobUpdateParam) {
  return request.put<Api.ScheduledJob.JobDTO>({
    url: `/api/scheduled-jobs/${jobCode}`,
    data
  })
}

export function fetchUpdateScheduledJobEnabled(jobCode: string, enabled: boolean) {
  return request.put<Api.ScheduledJob.JobDTO>({
    url: `/api/scheduled-jobs/${jobCode}/enabled`,
    data: { enabled }
  })
}

export function fetchRunScheduledJob(jobCode: string) {
  return request.post({
    url: `/api/scheduled-jobs/${jobCode}/run`
  })
}

export function fetchScheduledJobLogs(jobCode: string, params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.ScheduledJob.JobLogDTO>>({
    url: `/api/scheduled-jobs/${jobCode}/logs`,
    params
  })
}

export function fetchDeleteScheduledJobLogs(jobCode: string, ids: number[]) {
  return request.del({
    url: `/api/scheduled-jobs/${jobCode}/logs`,
    data: ids
  })
}
