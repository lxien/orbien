package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.oauth.OAuthFacade;
import io.github.lxien.orbien.server.web.oauth.OAuthPurpose;
import io.github.lxien.orbien.server.web.oauth.OAuthUrlBuilder;
import io.github.lxien.orbien.server.web.param.oauth.OAuthTicketParam;
import io.github.lxien.orbien.server.web.service.OAuthProviderConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthAuthController {

    private final OAuthFacade oauthFacade;
    private final OAuthProviderConfigService oauthProviderConfigService;
    private final OAuthUrlBuilder oauthUrlBuilder;

    @GetMapping("/providers")
    public Ajax listPublicProviders() {
        return Ajax.success(oauthProviderConfigService.listEnabledPublic());
    }

    @GetMapping("/authorize/{provider}")
    public void authorize(@PathVariable String provider,
                          @RequestParam(value = "return_origin", required = false) String returnOrigin,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        try {
            OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
            String url = oauthFacade.startAuthorize(providerId, OAuthPurpose.LOGIN, null, returnOrigin, request);
            response.sendRedirect(url);
        } catch (Exception e) {
            log.warn("OAuth authorize failed: provider={}, reason={}", provider, e.getMessage());
            String frontendOrigin = oauthUrlBuilder.resolveFrontendOrigin(request, returnOrigin);
            OAuthFacade.writeHtmlRedirect(response, oauthUrlBuilder.frontendLoginError(frontendOrigin, "failed"));
        }
    }

    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
        oauthFacade.handleCallback(providerId, code, state, error, request, response);
    }

    @PostMapping("/token")
    public Ajax exchangeToken(@Valid @RequestBody OAuthTicketParam param) {
        return Ajax.success(oauthFacade.exchangeTicket(param.getTicket()));
    }
}
