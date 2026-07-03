package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.server.web.common.crypto.CredentialEncryptor;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.utils.JsonUtils;
import com.xiaoniucode.etp.server.web.dto.dns.DnsCredentialDTO;
import com.xiaoniucode.etp.server.web.dto.dns.DnsProviderFieldDTO;
import com.xiaoniucode.etp.server.web.dto.dns.DnsProviderSchemaDTO;
import com.xiaoniucode.etp.server.web.entity.DnsCredentialDO;
import com.xiaoniucode.etp.server.web.enums.AcmeOrderStatus;
import com.xiaoniucode.etp.server.web.enums.DnsCredentialStatus;
import com.xiaoniucode.etp.server.web.enums.DnsProviderType;
import com.xiaoniucode.etp.server.web.param.dns.DnsCredentialSaveParam;
import com.xiaoniucode.etp.server.web.repository.AcmeCertOrderRepository;
import com.xiaoniucode.etp.server.web.repository.DnsCredentialRepository;
import com.xiaoniucode.etp.server.web.service.DnsCredentialService;
import com.xiaoniucode.etp.server.web.service.dns.DnsProviderAdapter;
import com.xiaoniucode.etp.server.web.service.dns.DnsProviderConfig;
import com.xiaoniucode.etp.server.web.service.dns.DnsProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DnsCredentialServiceImpl implements DnsCredentialService {

    private final DnsCredentialRepository dnsCredentialRepository;
    private final AcmeCertOrderRepository acmeCertOrderRepository;
    private final CredentialEncryptor credentialEncryptor;
    private final DnsProviderRegistry dnsProviderRegistry;

    @Override
    public List<DnsCredentialDTO> listAll() {
        return dnsCredentialRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public List<DnsProviderSchemaDTO> listProviderSchemas() {
        List<DnsProviderSchemaDTO> schemas = new ArrayList<>();
        schemas.add(schema(DnsProviderType.ALIYUN, List.of(
                field("accessKeyId", "AccessKey ID", true, false),
                field("accessKeySecret", "AccessKey Secret", true, true)
        )));
        schemas.add(schema(DnsProviderType.TENCENT, List.of(
                field("secretId", "SecretId", true, false),
                field("secretKey", "SecretKey", true, true)
        )));
        schemas.add(schema(DnsProviderType.CLOUDFLARE, List.of(
                field("apiToken", "API Token", true, true),
                field("zoneId", "Zone ID（可选）", false, false)
        )));
        return schemas;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DnsCredentialDTO save(DnsCredentialSaveParam param) {
        DnsProviderType provider = DnsProviderType.fromCode(param.getProvider());
        DnsProviderConfig config = mapConfig(provider, param.getConfig());
        DnsProviderAdapter adapter = dnsProviderRegistry.get(provider);
        adapter.testConnection(config);

        DnsCredentialDO entity = param.getId() == null
                ? new DnsCredentialDO()
                : dnsCredentialRepository.findById(param.getId()).orElseThrow(() -> new BizException("DNS密钥不存在"));
        entity.setName(param.getName());
        entity.setProvider(provider);
        entity.setConfigJson(credentialEncryptor.encrypt(JsonUtils.toJson(config)));
        entity.setAccountHint(buildAccountHint(provider, config));
        entity.setStatus(DnsCredentialStatus.ACTIVE);
        entity.setLastTestAt(LocalDateTime.now());
        entity.setLastTestMessage("连接成功");
        return toDTO(dnsCredentialRepository.save(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (acmeCertOrderRepository.existsByDnsCredentialIdAndStatusIn(id, List.of(
                AcmeOrderStatus.PENDING_DNS,
                AcmeOrderStatus.DNS_WAITING,
                AcmeOrderStatus.VALIDATING,
                AcmeOrderStatus.ISSUING))) {
            throw new BizException("该密钥存在进行中的证书申请，无法删除");
        }
        dnsCredentialRepository.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void test(Long id) {
        DnsCredentialDO entity = requireEntity(id);
        DnsProviderConfig config = decryptConfig(entity);
        DnsProviderAdapter adapter = dnsProviderRegistry.get(entity.getProvider());
        try {
            adapter.testConnection(config);
            entity.setStatus(DnsCredentialStatus.ACTIVE);
            entity.setLastTestMessage("连接成功");
        } catch (Exception e) {
            entity.setStatus(DnsCredentialStatus.INVALID);
            entity.setLastTestMessage(e.getMessage());
            throw new BizException(e.getMessage());
        } finally {
            entity.setLastTestAt(LocalDateTime.now());
            dnsCredentialRepository.save(entity);
        }
    }

    @Override
    public DnsProviderConfig resolveConfig(Long id) {
        return decryptConfig(requireEntity(id));
    }

    private DnsCredentialDO requireEntity(Long id) {
        return dnsCredentialRepository.findById(id).orElseThrow(() -> new BizException("DNS密钥不存在"));
    }

    private DnsProviderConfig decryptConfig(DnsCredentialDO entity) {
        return JsonUtils.fromJson(credentialEncryptor.decrypt(entity.getConfigJson()), DnsProviderConfig.class);
    }

    private DnsProviderConfig mapConfig(DnsProviderType provider, Map<String, String> config) {
        DnsProviderConfig result = new DnsProviderConfig();
        if (provider == DnsProviderType.ALIYUN) {
            result.setAccessKeyId(config.get("accessKeyId"));
            result.setAccessKeySecret(config.get("accessKeySecret"));
        } else if (provider == DnsProviderType.TENCENT) {
            result.setSecretId(config.get("secretId"));
            result.setSecretKey(config.get("secretKey"));
        } else if (provider == DnsProviderType.CLOUDFLARE) {
            result.setApiToken(config.get("apiToken"));
            result.setZoneId(config.get("zoneId"));
        }
        return result;
    }

    private String buildAccountHint(DnsProviderType provider, DnsProviderConfig config) {
        if (provider == DnsProviderType.ALIYUN) {
            return mask(config.getAccessKeyId());
        }
        if (provider == DnsProviderType.TENCENT) {
            return mask(config.getSecretId());
        }
        return "Token";
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 6) {
            return value.charAt(0) + "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 2);
    }

    private DnsCredentialDTO toDTO(DnsCredentialDO entity) {
        DnsCredentialDTO dto = new DnsCredentialDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setProvider(entity.getProvider().getCode());
        dto.setProviderLabel(entity.getProvider().getLabel());
        dto.setStatus(entity.getStatus().getCode());
        dto.setAccountHint(entity.getAccountHint());
        dto.setLastTestAt(entity.getLastTestAt());
        dto.setLastTestMessage(entity.getLastTestMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private DnsProviderSchemaDTO schema(DnsProviderType provider, List<DnsProviderFieldDTO> fields) {
        DnsProviderSchemaDTO dto = new DnsProviderSchemaDTO();
        dto.setProvider(provider.getCode());
        dto.setLabel(provider.getLabel());
        dto.setFields(fields);
        return dto;
    }

    private DnsProviderFieldDTO field(String key, String label, boolean required, boolean secret) {
        DnsProviderFieldDTO field = new DnsProviderFieldDTO();
        field.setKey(key);
        field.setLabel(label);
        field.setType(secret ? "password" : "text");
        field.setRequired(required);
        field.setSecret(secret);
        return field;
    }
}
