package com.xiaoniucode.etp.server.web.service.dns;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.dnspod.v20210323.DnspodClient;
import com.tencentcloudapi.dnspod.v20210323.models.*;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.enums.DnsProviderType;
import com.xiaoniucode.etp.server.web.service.acme.DnsNameHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TencentDnsAdapter implements DnsProviderAdapter {

    @Override
    public DnsProviderType providerType() {
        return DnsProviderType.TENCENT;
    }

    @Override
    public void testConnection(DnsProviderConfig config) {
        validate(config);
        try {
            DescribeDomainListRequest request = new DescribeDomainListRequest();
            request.setLimit(1L);
            client(config).DescribeDomainList(request);
        } catch (TencentCloudSDKException e) {
            throw new BizException("腾讯云DNS连接失败: " + e.getMessage());
        }
    }

    @Override
    public String resolveZone(String fqdn, DnsProviderConfig config) {
        validate(config);
        String candidate = DnsNameHelper.normalize(fqdn);
        while (StringUtils.hasText(candidate)) {
            if (zoneExists(candidate, config)) {
                return candidate;
            }
            int idx = candidate.indexOf('.');
            if (idx < 0) {
                break;
            }
            candidate = candidate.substring(idx + 1);
        }
        throw new BizException("无法在腾讯云找到域名区域: " + fqdn);
    }

    @Override
    public DnsRecordRef addTxtRecord(String fqdn, String recordName, String value, DnsProviderConfig config) {
        validate(config);
        String zone = resolveZone(fqdn, config);
        String subDomain = DnsNameHelper.splitHostRecord(recordName, zone);
        try {
            CreateRecordRequest request = new CreateRecordRequest();
            request.setDomain(zone);
            request.setSubDomain(subDomain);
            request.setRecordType("TXT");
            request.setRecordLine("默认");
            request.setValue(value);
            request.setTTL(600L);
            CreateRecordResponse response = client(config).CreateRecord(request);
            return new DnsRecordRef(String.valueOf(response.getRecordId()), zone);
        } catch (TencentCloudSDKException e) {
            throw new BizException("腾讯云添加TXT记录失败: " + e.getMessage());
        }
    }

    @Override
    public void removeTxtRecord(DnsRecordRef recordRef, DnsProviderConfig config) {
        if (recordRef == null || !StringUtils.hasText(recordRef.providerRecordId())) {
            return;
        }
        validate(config);
        try {
            DeleteRecordRequest request = new DeleteRecordRequest();
            request.setDomain(recordRef.zone());
            request.setRecordId(Long.parseLong(recordRef.providerRecordId()));
            client(config).DeleteRecord(request);
        } catch (TencentCloudSDKException e) {
            if (isRecordAlreadyRemoved(e)) {
                return;
            }
            throw new BizException("腾讯云删除TXT记录失败: " + e.getMessage());
        }
    }

    private boolean isRecordAlreadyRemoved(TencentCloudSDKException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        String errorCode = e.getErrorCode() != null ? e.getErrorCode() : "";
        return message.contains("记录编号错误")
                || message.contains("记录不存在")
                || errorCode.contains("ResourceNotFound")
                || errorCode.contains("InvalidParameterValue");
    }

    private boolean zoneExists(String zone, DnsProviderConfig config) {
        try {
            DescribeRecordListRequest request = new DescribeRecordListRequest();
            request.setDomain(zone);
            request.setLimit(1L);
            client(config).DescribeRecordList(request);
            return true;
        } catch (TencentCloudSDKException e) {
            return false;
        }
    }

    private DnspodClient client(DnsProviderConfig config) {
        return new DnspodClient(new Credential(config.getSecretId(), config.getSecretKey()), "");
    }

    private void validate(DnsProviderConfig config) {
        if (!StringUtils.hasText(config.getSecretId()) || !StringUtils.hasText(config.getSecretKey())) {
            throw new BizException("腾讯云 SecretId/SecretKey 未配置完整");
        }
    }
}
