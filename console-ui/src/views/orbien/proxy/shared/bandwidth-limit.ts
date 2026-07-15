import type {FormItemRule} from 'element-plus'

export type LimitTotalMbps = number | null | undefined

export const LIMIT_TOTAL_RULES: FormItemRule[] = [
    {
        validator: (_rule, value: LimitTotalMbps, callback) => {
            if (value == null) {
                callback()
                return
            }
            if (typeof value !== 'number' || Number.isNaN(value) || value < 1 || !Number.isInteger(value)) {
                callback(new Error('带宽必须为大于 0 的整数'))
                return
            }
            callback()
        },
        trigger: 'blur'
    }
]

export function toLimitTotalPayload(value: LimitTotalMbps): number | null {
    if (value == null || Number.isNaN(value)) {
        return null
    }
    return value
}
