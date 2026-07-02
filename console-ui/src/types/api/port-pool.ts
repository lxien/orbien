declare namespace Api.PortPool {
  interface PortPoolDTO {
    id: number
    startPort: number
    endPort?: number
    type: number
    remark?: string
    createdAt: string
    updatedAt: string
  }

  interface PortPoolCreateParam {
    port: string
    type: number
    remark?: string
  }

  interface PortPoolUpdateParam {
    id: number
    port: string
    type: number
    remark?: string
  }
}
