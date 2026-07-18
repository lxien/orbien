package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dns.crypto.CredentialEncryptor;
import io.github.lxien.orbien.server.web.dto.oauth.OAuthProviderConfigDTO;
import io.github.lxien.orbien.server.web.dto.oauth.OAuthPublicProviderDTO;
import io.github.lxien.orbien.server.web.entity.OAuthProviderDO;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.oauth.OAuthUrlBuilder;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderEnableParam;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderSaveParam;
import io.github.lxien.orbien.server.web.repository.OAuthProviderRepository;
import io.github.lxien.orbien.server.web.service.OAuthProviderConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthProviderConfigServiceImpl implements OAuthProviderConfigService {

    private final OAuthProviderRepository oauthProviderRepository;
    private final CredentialEncryptor credentialEncryptor;
    private final OAuthUrlBuilder oauthUrlBuilder;

    @Override
    public List<OAuthPublicProviderDTO> listEnabledPublic() {
        List<OAuthPublicProviderDTO> result = new ArrayList<>();
        for (OAuthProviderId provider : OAuthProviderId.values()) {
            oauthProviderRepository.findByProvider(provider.name()).ifPresent(entity -> {
                if (Boolean.TRUE.equals(entity.getEnabled())
                        && StringUtils.hasText(entity.getClientId())
                        && StringUtils.hasText(entity.getClientSecretEnc())) {
                    result.add(new OAuthPublicProviderDTO(provider.name(), provider.getDisplayName()));
                }
            });
        }
        return result;
    }

    @Override
    public List<OAuthProviderConfigDTO> listAll(HttpServletRequest request) {
        List<OAuthProviderConfigDTO> result = new ArrayList<>();
        for (OAuthProviderId provider : OAuthProviderId.values()) {
            result.add(toDto(provider,
                    oauthProviderRepository.findByProvider(provider.name()).orElse(null),
                    request));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuthProviderConfigDTO save(OAuthProviderId provider, OAuthProviderSaveParam param, HttpServletRequest request) {
        OAuthProviderDO entity = oauthProviderRepository.findByProvider(provider.name())
                .orElseGet(OAuthProviderDO::new);
        entity.setProvider(provider.name());
        if (entity.getEnabled() == null) {
            entity.setEnabled(false);
        }

        if (StringUtils.hasText(param.getClientId())) {
            entity.setClientId(param.getClientId().trim());
        }
        if (StringUtils.hasText(param.getClientSecret())) {
            entity.setClientSecretEnc(credentialEncryptor.encrypt(param.getClientSecret().trim()));
        }

        if (!StringUtils.hasText(entity.getClientId())) {
            throw new BizException("Client ID 不能为空");
        }
        if (!StringUtils.hasText(entity.getClientSecretEnc())) {
            throw new BizException("Client Secret 不能为空");
        }

        return toDto(provider, oauthProviderRepository.save(entity), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuthProviderConfigDTO updateEnabled(OAuthProviderId provider,
                                                OAuthProviderEnableParam param,
                                                HttpServletRequest request) {
        OAuthProviderDO entity = oauthProviderRepository.findByProvider(provider.name())
                .orElseThrow(() -> new BizException(provider.getDisplayName() + " 尚未配置凭证"));

        boolean enabled = Boolean.TRUE.equals(param.getEnabled());
        if (enabled) {
            if (!StringUtils.hasText(entity.getClientId()) || !StringUtils.hasText(entity.getClientSecretEnc())) {
                throw new BizException("启用前须完成 Client ID 与 Client Secret 配置");
            }
        }
        entity.setEnabled(enabled);
        return toDto(provider, oauthProviderRepository.save(entity), request);
    }

    @Override
    public ResolvedCredentials requireConfiguredCredentials(OAuthProviderId provider) {
        OAuthProviderDO entity = oauthProviderRepository.findByProvider(provider.name())
                .orElseThrow(() -> new BizException(provider.getDisplayName() + " 未配置"));
        if (!StringUtils.hasText(entity.getClientId()) || !StringUtils.hasText(entity.getClientSecretEnc())) {
            throw new BizException(provider.getDisplayName() + " 凭证不完整");
        }
        return new ResolvedCredentials(entity.getClientId(), credentialEncryptor.decrypt(entity.getClientSecretEnc()));
    }

    @Override
    public ResolvedCredentials requireEnabledCredentials(OAuthProviderId provider) {
        ResolvedCredentials credentials = requireConfiguredCredentials(provider);
        OAuthProviderDO entity = oauthProviderRepository.findByProvider(provider.name())
                .orElseThrow(() -> new BizException(provider.getDisplayName() + " 未配置"));
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BizException(provider.getDisplayName() + " 未启用");
        }
        return credentials;
    }

    private OAuthProviderConfigDTO toDto(OAuthProviderId provider, OAuthProviderDO entity, HttpServletRequest request) {
        OAuthProviderConfigDTO dto = new OAuthProviderConfigDTO();
        dto.setProvider(provider.name());
        dto.setDisplayName(provider.getDisplayName());
        dto.setCallbackUrl(oauthUrlBuilder.callbackUrl(request, provider));
        if (entity == null) {
            dto.setEnabled(false);
            dto.setClientId("");
            dto.setSecretConfigured(false);
            return dto;
        }
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        dto.setClientId(entity.getClientId() == null ? "" : entity.getClientId());
        dto.setSecretConfigured(StringUtils.hasText(entity.getClientSecretEnc()));
        return dto;
    }
}
