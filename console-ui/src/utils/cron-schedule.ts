export type CronScheduleType =
  | 'daily'
  | 'everyNDays'
  | 'hourly'
  | 'everyNHours'
  | 'everyNMinutes'
  | 'weekly'
  | 'monthly'
  | 'everyNSeconds'
  | 'custom'

export interface CronSchedule {
  type: CronScheduleType
  minute?: number
  hour?: number
  dayOfMonth?: number
  dayOfWeek?: number
  interval?: number
  customExpression?: string
}

export const CRON_SCHEDULE_TYPE_OPTIONS: { label: string; value: CronScheduleType }[] = [
  { label: '每天', value: 'daily' },
  { label: 'N 天', value: 'everyNDays' },
  { label: '每小时', value: 'hourly' },
  { label: 'N 小时', value: 'everyNHours' },
  { label: 'N 分钟', value: 'everyNMinutes' },
  { label: '每周', value: 'weekly' },
  { label: '每月', value: 'monthly' },
  { label: 'N 秒', value: 'everyNSeconds' },
  { label: '自定义', value: 'custom' }
]

export const WEEKDAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

const WEEKDAY_TO_CRON = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

function pad(value?: number): string {
  return String(value ?? 0).padStart(2, '0')
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function uiDayToCron(dayOfWeek: number): string {
  return WEEKDAY_TO_CRON[clamp(dayOfWeek, 1, 7) - 1]
}

function cronDayToUi(dayOfWeek: string): number {
  const named: Record<string, number> = {
    MON: 1,
    TUE: 2,
    WED: 3,
    THU: 4,
    FRI: 5,
    SAT: 6,
    SUN: 7
  }
  const upper = dayOfWeek.toUpperCase()
  if (named[upper]) {
    return named[upper]
  }
  const numeric = Number(dayOfWeek)
  if (numeric === 0 || numeric === 7) {
    return 7
  }
  if (numeric >= 1 && numeric <= 6) {
    return numeric
  }
  return 1
}

function parsePositiveInt(value: string, fallback = 0): number {
  const parsed = Number.parseInt(value, 10)
  return Number.isFinite(parsed) ? parsed : fallback
}

export function createDefaultSchedule(type: CronScheduleType): CronSchedule {
  switch (type) {
    case 'everyNDays':
      return { type, hour: 1, minute: 30, interval: 1 }
    case 'hourly':
      return { type, minute: 0 }
    case 'everyNHours':
      return { type, minute: 0, interval: 1 }
    case 'everyNMinutes':
      return { type, interval: 5 }
    case 'weekly':
      return { type, dayOfWeek: 1, hour: 1, minute: 30 }
    case 'monthly':
      return { type, dayOfMonth: 1, hour: 1, minute: 30 }
    case 'everyNSeconds':
      return { type, interval: 30 }
    case 'custom':
      return { type, customExpression: '0 0 1 * * ?' }
    case 'daily':
    default:
      return { type: 'daily', hour: 1, minute: 30 }
  }
}

export function buildCronExpression(schedule: CronSchedule): string {
  switch (schedule.type) {
    case 'daily':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} ${clamp(schedule.hour ?? 0, 0, 23)} * * ?`
    case 'everyNDays':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} ${clamp(schedule.hour ?? 0, 0, 23)} 1/${clamp(schedule.interval ?? 1, 1, 31)} * ?`
    case 'hourly':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} * * * ?`
    case 'everyNHours':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} */${clamp(schedule.interval ?? 1, 1, 23)} * * ?`
    case 'everyNMinutes':
      return `0 */${clamp(schedule.interval ?? 1, 1, 59)} * * * ?`
    case 'weekly':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} ${clamp(schedule.hour ?? 0, 0, 23)} ? * ${uiDayToCron(schedule.dayOfWeek ?? 1)}`
    case 'monthly':
      return `0 ${clamp(schedule.minute ?? 0, 0, 59)} ${clamp(schedule.hour ?? 0, 0, 23)} ${clamp(schedule.dayOfMonth ?? 1, 1, 31)} * ?`
    case 'everyNSeconds':
      return `*/${clamp(schedule.interval ?? 1, 1, 59)} * * * * ?`
    case 'custom':
      return schedule.customExpression?.trim() || '0 0 1 * * ?'
    default:
      return '0 0 1 * * ?'
  }
}

export function parseCronExpression(cron: string): CronSchedule {
  const expression = cron?.trim()
  if (!expression) {
    return createDefaultSchedule('daily')
  }

  const parts = expression.split(/\s+/)
  if (parts.length !== 6) {
    return { type: 'custom', customExpression: expression }
  }

  const [sec, min, hour, dom, month, dow] = parts

  if (sec.startsWith('*/') && min === '*' && hour === '*' && dom === '*' && month === '*' && dow === '?') {
    return { type: 'everyNSeconds', interval: parsePositiveInt(sec.slice(2), 1) }
  }

  if (sec === '0' && min.startsWith('*/') && hour === '*' && dom === '*' && month === '*' && dow === '?') {
    return { type: 'everyNMinutes', interval: parsePositiveInt(min.slice(2), 1) }
  }

  if (sec === '0' && !min.includes('/') && hour.startsWith('*/') && dom === '*' && month === '*' && dow === '?') {
    return {
      type: 'everyNHours',
      minute: parsePositiveInt(min, 0),
      interval: parsePositiveInt(hour.slice(2), 1)
    }
  }

  if (sec === '0' && !min.includes('/') && hour === '*' && dom === '*' && month === '*' && dow === '?') {
    return { type: 'hourly', minute: parsePositiveInt(min, 0) }
  }

  if (sec === '0' && dom === '?' && month === '*' && dow !== '?' && dow !== '*') {
    return {
      type: 'weekly',
      minute: parsePositiveInt(min, 0),
      hour: parsePositiveInt(hour, 0),
      dayOfWeek: cronDayToUi(dow)
    }
  }

  if (sec === '0' && month === '*' && dow === '?' && dom !== '*' && !dom.includes('/')) {
    return {
      type: 'monthly',
      minute: parsePositiveInt(min, 0),
      hour: parsePositiveInt(hour, 0),
      dayOfMonth: parsePositiveInt(dom, 1)
    }
  }

  if (sec === '0' && dom.startsWith('1/') && month === '*' && dow === '?') {
    return {
      type: 'everyNDays',
      minute: parsePositiveInt(min, 0),
      hour: parsePositiveInt(hour, 0),
      interval: parsePositiveInt(dom.slice(2), 1)
    }
  }

  if (
    sec === '0' &&
    !min.includes('/') &&
    !hour.includes('/') &&
    dom === '*' &&
    month === '*' &&
    dow === '?'
  ) {
    return {
      type: 'daily',
      minute: parsePositiveInt(min, 0),
      hour: parsePositiveInt(hour, 0)
    }
  }

  return { type: 'custom', customExpression: expression }
}

export function describeCronSchedule(schedule: CronSchedule): string {
  switch (schedule.type) {
    case 'daily':
      return `每天的 ${pad(schedule.hour)}:${pad(schedule.minute)} 执行一次`
    case 'everyNDays':
      return `每 ${schedule.interval ?? 1} 天的 ${pad(schedule.hour)}:${pad(schedule.minute)} 执行一次`
    case 'hourly':
      return `每小时的第 ${schedule.minute ?? 0} 分钟执行一次`
    case 'everyNHours':
      return `每 ${schedule.interval ?? 1} 小时的第 ${schedule.minute ?? 0} 分钟执行一次`
    case 'everyNMinutes':
      return `每 ${schedule.interval ?? 1} 分钟执行一次`
    case 'weekly': {
      const weekday = WEEKDAY_OPTIONS.find((item) => item.value === schedule.dayOfWeek)?.label || '周一'
      return `每周 ${weekday} ${pad(schedule.hour)}:${pad(schedule.minute)} 执行一次`
    }
    case 'monthly':
      return `每月 ${schedule.dayOfMonth ?? 1} 号 ${pad(schedule.hour)}:${pad(schedule.minute)} 执行一次`
    case 'everyNSeconds':
      return `每 ${schedule.interval ?? 1} 秒执行一次`
    case 'custom':
      return schedule.customExpression || '自定义 Cron 表达式'
    default:
      return '未配置执行周期'
  }
}

export function describeCronExpression(cron: string): string {
  return describeCronSchedule(parseCronExpression(cron))
}
