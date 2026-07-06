/**
 * 多后端（负载均衡）模式：targets 数量大于 1
 * */
export function isClusterMode(targets?: Api.Proxy.TargetDTO[]): boolean {
    return (targets?.length ?? 0) > 1
}
