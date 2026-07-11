export function formatAgentOptionLabel(agent: Api.Agent.AgentDTO): string {
  const shortId = agent.id.length > 8 ? agent.id.slice(-8) : agent.id
  const statusLabel = agent.isOnline ? '在线' : '离线'
  return `${agent.name} · ${statusLabel} · ${shortId}`
}
