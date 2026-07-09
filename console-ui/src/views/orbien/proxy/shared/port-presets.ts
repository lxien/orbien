export interface PortPresetOption {
    label: string
    value: number
}

/** HTTP / HTTPS / TCP 常用内网端口 */
export const COMMON_LOCAL_PORT_PRESETS: PortPresetOption[] = [
    {label: 'HTTP - 80', value: 80},
    {label: 'HTTPS - 443', value: 443},
    {label: 'SSH - 22', value: 22},
    {label: 'Redis - 6379', value: 6379},
    {label: 'Tomcat - 8080', value: 8080},
    {label: 'MySQL - 3306', value: 3306},
    {label: 'SQL Server - 1433', value: 1433},
    {label: 'Windows远程桌面 - 3389', value: 3389}
]

/** UDP 常用内网端口 */
export const UDP_LOCAL_PORT_PRESETS: PortPresetOption[] = [
    {label: 'DNS - 53', value: 53},
    {label: 'NTP - 123', value: 123},
    {label: 'SNMP - 161', value: 161}
]

export function matchPortPreset(
    port: number | undefined,
    presets: PortPresetOption[]
): number | undefined {
    if (port == null) return undefined
    return presets.some((item) => item.value === port) ? port : undefined
}
