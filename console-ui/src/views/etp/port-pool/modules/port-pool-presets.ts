/** 端口池应用类型模板 */
export const CUSTOM_PRESET_ID = 'custom'

export interface PortPoolPreset {
  id: string
  label: string
  type: 1 | 2
  port: string
  remark: string
  group: string
}

/** type: 1=TCP, 2=UDP */
export const PORT_POOL_PRESETS: PortPoolPreset[] = [
  { id: 'ssh', label: 'SSH (22)', type: 1, port: '22', remark: 'SSH 远程登录', group: '远程登录' },
  { id: 'rdp', label: 'RDP (3389)', type: 1, port: '3389', remark: 'Windows 远程桌面', group: '远程登录' },
  { id: 'http', label: 'HTTP (80)', type: 1, port: '80', remark: 'HTTP Web 服务', group: 'Web 服务' },
  { id: 'https', label: 'HTTPS (443)', type: 1, port: '443', remark: 'HTTPS 加密 Web 服务', group: 'Web 服务' },
  { id: 'mysql', label: 'MySQL (3306)', type: 1, port: '3306', remark: 'MySQL 数据库', group: '数据库' },
  {
    id: 'postgresql',
    label: 'PostgreSQL (5432)',
    type: 1,
    port: '5432',
    remark: 'PostgreSQL 数据库',
    group: '数据库'
  },
  {
    id: 'sqlserver',
    label: 'SQL Server (1433)',
    type: 1,
    port: '1433',
    remark: 'Microsoft SQL Server',
    group: '数据库'
  },
  { id: 'redis', label: 'Redis (6379)', type: 1, port: '6379', remark: 'Redis 缓存服务', group: '数据库' },
  {
    id: 'mongodb',
    label: 'MongoDB (27017)',
    type: 1,
    port: '27017',
    remark: 'MongoDB 数据库',
    group: '数据库'
  },
  { id: 'ftp', label: 'FTP (21)', type: 1, port: '21', remark: 'FTP 文件传输', group: '文件传输' },
  { id: 'dns', label: 'DNS (53)', type: 2, port: '53', remark: 'DNS 域名解析', group: '网络服务' },
  { id: 'ntp', label: 'NTP (123)', type: 2, port: '123', remark: 'NTP 时间同步', group: '网络服务' },
  { id: 'snmp', label: 'SNMP (161)', type: 2, port: '161', remark: 'SNMP 网络管理', group: '网络服务' }
]

export const PORT_POOL_PRESET_GROUPS = [...new Set(PORT_POOL_PRESETS.map((item) => item.group))]
