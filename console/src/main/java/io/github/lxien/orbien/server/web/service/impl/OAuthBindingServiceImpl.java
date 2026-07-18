package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.oauth.OAuthBindingDTO;
import io.github.lxien.orbien.server.web.entity.OAuthBindingDO;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.oauth.OAuthUserProfile;
import io.github.lxien.orbien.server.web.repository.OAuthBindingRepository;
import io.github.lxien.orbien.server.web.service.OAuthBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuthBindingServiceImpl implements OAuthBindingService {

    private final OAuthBindingRepository oauthBindingRepository;

    @Override
    public List<OAuthBindingDTO> listForUser(String username) {
        Map<String, OAuthBindingDO> byProvider = oauthBindingRepository.findByUsername(username).stream()
                .collect(Collectors.toMap(OAuthBindingDO::getProvider, Function.identity(), (a, b) -> a));
        List<OAuthBindingDTO> result = new ArrayList<>();
        for (OAuthProviderId provider : OAuthProviderId.values()) {
            OAuthBindingDTO dto = new OAuthBindingDTO();
            dto.setProvider(provider.name());
            dto.setDisplayName(provider.getDisplayName());
            OAuthBindingDO entity = byProvider.get(provider.name());
            if (entity == null) {
                dto.setBound(false);
            } else {
                dto.setBound(true);
                dto.setExternalLogin(entity.getExternalLogin());
                dto.setBoundAt(entity.getBoundAt());
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public Optional<String> findUsername(OAuthProviderId provider, String externalId) {
        return oauthBindingRepository.findByProviderAndExternalId(provider.name(), externalId)
                .map(OAuthBindingDO::getUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(String username, OAuthProviderId provider, OAuthUserProfile profile) {
        oauthBindingRepository.findByProviderAndExternalId(provider.name(), profile.externalId())
                .ifPresent(existing -> {
                    if (!username.equals(existing.getUsername())) {
                        throw new BizException("该 " + provider.getDisplayName() + " 账号已绑定其他用户");
                    }
                });

        OAuthBindingDO entity = oauthBindingRepository.findByUsernameAndProvider(username, provider.name())
                .orElseGet(OAuthBindingDO::new);
        entity.setUsername(username);
        entity.setProvider(provider.name());
        entity.setExternalId(profile.externalId());
        entity.setExternalLogin(profile.externalLogin());
        oauthBindingRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(String username, OAuthProviderId provider) {
        oauthBindingRepository.findByUsernameAndProvider(username, provider.name())
                .orElseThrow(() -> new BizException("未绑定 " + provider.getDisplayName()));
        oauthBindingRepository.deleteByUsernameAndProvider(username, provider.name());
    }
}
