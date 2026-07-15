declare namespace Api.TimeAccess {
  interface WindowDTO {
    id?: number
    start: string
    end: string
  }

  interface DetailDTO {
    proxyId: string
    enabled: boolean
    mode: number
    timeEnabled: boolean
    timezone: string
    days: number[]
    windows: WindowDTO[]
  }

  interface UpdateParam {
    proxyId: string
    enabled: boolean
    mode: number
    timeEnabled: boolean
    timezone?: string
    days?: number[]
  }

  interface WindowAddParam {
    proxyId: string
    start: string
    end: string
  }

  interface WindowUpdateParam {
    id: number
    proxyId: string
    start: string
    end: string
  }
}
