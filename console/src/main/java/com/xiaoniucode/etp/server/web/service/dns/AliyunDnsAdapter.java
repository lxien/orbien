package com.xiaoniucode.etp.server.web.service.dns;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.profile.DefaultProfile;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.enums.DnsProviderType;
import com.xiaoniucode.etp.server.web.service.acme.DnsNameHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AliyunDnsAdapter implements DnsProviderAdapter {

    @Override
    public DnsProviderType providerType() {
        return DnsProviderType.ALIYUN;
    }

    @Override
    public void testConnection(DnsProviderConfig config) {
        validate(config);
        IAcsClient client = client(config);
        try {
            DescribeDomainsRequest request = new DescribeDomainsRequest();
            request.setPageSize(1L);
            client.getAcsResponse(request);
        } catch (Exception e) {
            throw new BizException("阿里云DNS连接失败: " + e.getMessage());
        }
    }

    @Override
    public String resolveZone(String fqdn, DnsProviderConfig config) {
        validate(config);
        IAcsClient client = client(config);
        String candidate = DnsNameHelper.normalize(fqdn);
        while (StringUtils.hasText(candidate)) {
            if (zoneExists(client, candidate)) {
                return candidate;
            }
            int idx = candidate.indexOf('.');
            if (idx < 0) {
                break;
            }
            candidate = candidate.substring(idx + 1);
        }
        throw new BizException("无法在阿里云找到域名区域: " + fqdn);
    }

    @Override
    public DnsRecordRef addTxtRecord(String fqdn, String recordName, String value, DnsProviderConfig config) {
        validate(config);
        String zone = resolveZone(fqdn, config);
        String rr = DnsNameHelper.splitHostRecord(recordName, zone);
        try {
            AddDomainRecordRequest request = new AddDomainRecordRequest();
            request.setDomainName(zone);
            request.setRR(rr);
            request.setType("TXT");
            request.setValue(value);
            request.setTTL(600L);
            AddDomainRecordResponse response = client(config).getAcsResponse(request);
            return new DnsRecordRef(response.getRecordId(), zone);
        } catch (Exception e) {
            throw new BizException("阿里云添加TXT记录失败: " + e.getMessage());
        }
    }

    @Override
    public void removeTxtRecord(DnsRecordRef recordRef, DnsProviderConfig config) {
        if (recordRef == null || !StringUtils.hasText(recordRef.providerRecordId())) {
            return;
        }
        validate(config);
        try {
            DeleteDomainRecordRequest request = new DeleteDomainRecordRequest();
            request.setRecordId(recordRef.providerRecordId());
            client(config).getAcsResponse(request);
        } catch (Exception e) {
            throw new BizException("阿里云删除TXT记录失败: " + e.getMessage());
        }
    }

    private boolean zoneExists(IAcsClient client, String zone) {
        try {
            DescribeDomainsRequest request = new DescribeDomainsRequest();
            request.setKeyWord(zone);
            request.setSearchMode("EXACT");
            DescribeDomainsResponse response = client.getAcsResponse(request);
            return response.getDomains() != null && !response.getDomains().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private IAcsClient client(DnsProviderConfig config) {
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", config.getAccessKeyId(), config.getAccessKeySecret());
        return new DefaultAcsClient(profile);
    }

    private void validate(DnsProviderConfig config) {
        if (!StringUtils.hasText(config.getAccessKeyId()) || !StringUtils.hasText(config.getAccessKeySecret())) {
            throw new BizException("阿里云 AccessKey 未配置完整");
        }
    }
}
