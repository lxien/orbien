package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.oauth.OAuthBindingDTO;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.oauth.OAuthUserProfile;

import java.util.List;
import java.util.Optional;

public interface OAuthBindingService {

    List<OAuthBindingDTO> listForUser(String username);

    Optional<String> findUsername(OAuthProviderId provider, String externalId);

    void bind(String username, OAuthProviderId provider, OAuthUserProfile profile);

    void unbind(String username, OAuthProviderId provider);
}
