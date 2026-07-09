import {h} from 'vue'
import {ElTag} from 'element-plus'

export type TlsCertSummaryTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

export interface TlsCertSummaryDisplay {
    text: string
    type: TlsCertSummaryTagType
}

export function resolveTlsCertSummaryDisplay(
    summary?: Api.Proxy.TlsCertSummary | null
): TlsCertSummaryDisplay {
    if (!summary || summary.totalDomains === 0) {
        return {text: '—', type: 'info'}
    }

    const {totalDomains, deployedCount, warningCount} = summary

    if (deployedCount === totalDomains) {
        return {text: '已部署', type: 'primary'}
    }

    if (deployedCount === 0) {
        return warningCount > 0
            ? {text: '部署异常', type: 'danger'}
            : {text: '未部署', type: 'info'}
    }

    if (warningCount > 0) {
        return {
            text: `${deployedCount}/${totalDomains} 已部署`,
            type: 'warning'
        }
    }

    return {
        text: `${deployedCount}/${totalDomains} 已部署`,
        type: 'warning'
    }
}

export function renderTlsCertSummaryTag(
    summary?: Api.Proxy.TlsCertSummary | null,
    onClick?: () => void
) {
    const {text, type} = resolveTlsCertSummaryDisplay(summary)
    const clickable = Boolean(onClick) && text !== '—'

    return h(
        ElTag,
        {
            type,
            size: 'small',
            style: clickable ? 'cursor: pointer;' : undefined,
            onClick: clickable ? onClick : undefined
        },
        () => text
    )
}
