package io.github.lxien.orbien.server.web.service.inspector;

import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecordSummary;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordDTO;
import io.github.lxien.orbien.server.web.dto.inspector.HttpCaptureRecordSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class InspectorRecordConverter {

    public HttpCaptureRecordSummaryDTO toSummaryDto(HttpCaptureRecordSummary summary) {
        if (summary == null) {
            return null;
        }
        HttpCaptureRecordSummaryDTO dto = new HttpCaptureRecordSummaryDTO();
        dto.setId(summary.getId());
        dto.setProxyId(summary.getProxyId());
        dto.setStreamId(summary.getStreamId());
        dto.setStartedAt(summary.getStartedAt());
        dto.setDurationMs(summary.getDurationMs());
        dto.setMethod(summary.getMethod());
        dto.setPath(summary.getPath());
        dto.setHost(summary.getHost());
        dto.setScheme(summary.getScheme());
        dto.setStatus(summary.getStatus());
        dto.setStatusText(summary.getStatusText());
        dto.setReplay(summary.isReplay());
        dto.setSourceRecordId(summary.getSourceRecordId());
        return dto;
    }

    public HttpCaptureRecordDTO toDetailDto(HttpCaptureRecord record) {
        if (record == null) {
            return null;
        }
        HttpCaptureRecordDTO dto = new HttpCaptureRecordDTO();
        dto.setId(record.getId());
        dto.setProxyId(record.getProxyId());
        dto.setStreamId(record.getStreamId());
        dto.setStartedAt(record.getStartedAt());
        dto.setDurationMs(record.getDurationMs());
        dto.setClientIp(record.getClientIp());
        dto.setHost(record.getHost());
        dto.setMethod(record.getMethod());
        dto.setPath(record.getPath());
        dto.setScheme(record.getScheme());
        dto.setStatus(record.getStatus());
        dto.setStatusText(record.getStatusText());
        dto.setRequestHeaders(record.getRequestHeaders());
        dto.setResponseHeaders(record.getResponseHeaders());
        dto.setRequestBodySize(record.getRequestBodySize());
        dto.setResponseBodySize(record.getResponseBodySize());
        dto.setRequestBodyPreview(record.getRequestBodyPreview());
        dto.setResponseBodyPreview(record.getResponseBodyPreview());
        dto.setRequestBodyTruncated(record.isRequestBodyTruncated());
        dto.setResponseBodyTruncated(record.isResponseBodyTruncated());
        dto.setRawRequest(record.getRawRequest());
        dto.setRawResponse(record.getRawResponse());
        dto.setReplay(record.isReplay());
        dto.setSourceRecordId(record.getSourceRecordId());
        return dto;
    }
}
