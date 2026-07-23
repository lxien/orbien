declare namespace Api.Inspector {
    interface RecordSummary {
        id: string
        proxyId: string
        streamId: number
        startedAt: string
        durationMs: number
        method: string
        path: string
        host: string
        scheme: string
        status: number
        statusText: string
        replay?: boolean
        sourceRecordId?: string
    }

    interface RecordDetail extends RecordSummary {
        clientIp: string
        requestHeaders: Record<string, string>
        responseHeaders: Record<string, string>
        requestBodySize: number
        responseBodySize: number
        requestBodyPreview?: string
        responseBodyPreview?: string
        requestBodyTruncated: boolean
        responseBodyTruncated: boolean
        rawRequest?: string
        rawResponse?: string
    }

    interface Config {
        proxyId: string
        inspectorEnabled: boolean
    }

    interface ConfigUpdateParam {
        proxyId: string
        inspectorEnabled: boolean
    }

    interface ReplayOverrides {
        method?: string
        path?: string
        headers?: Record<string, string>
        body?: string
    }

    interface ReplayParam {
        captureToBuffer?: boolean
        timeoutSeconds?: number
        overrides?: ReplayOverrides
    }

    interface ReplayResult {
        sourceRecordId: string
        replayRecordId: string
        modified: boolean
        status: string
        record: RecordDetail
    }
}
