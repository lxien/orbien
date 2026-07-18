package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.oauth.OAuthFacade;
import io.github.lxien.orbien.server.web.oauth.OAuthPurpose;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderEnableParam;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderSaveParam;
import io.github.lxien.orbien.server.web.security.SecurityUtils;
import io.github.lxien.orbien.server.web.service.OAuthBindingService;
import io.github.lxien.orbien.server.web.service.OAuthProviderConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthAdminController {

    private final OAuthProviderConfigService oauthProviderConfigService;
    private final OAuthBindingService oauthBindingService;
    private final OAuthFacade oauthFacade;

    @GetMapping("/providers")
    public Ajax listProviders(HttpServletRequest request) {
        return Ajax.success(oauthProviderConfigService.listAll(request));
    }

    @PutMapping("/providers/{provider}")
    public Ajax saveProvider(@PathVariable String provider,
                             @Valid @RequestBody OAuthProviderSaveParam param,
                             HttpServletRequest request) {
        OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
        return Ajax.success(oauthProviderConfigService.save(providerId, param, request));
    }

    @PutMapping("/providers/{provider}/enabled")
    public Ajax updateEnabled(@PathVariable String provider,
                              @Valid @RequestBody OAuthProviderEnableParam param,
                              HttpServletRequest request) {
        OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
        return Ajax.success(oauthProviderConfigService.updateEnabled(providerId, param, request));
    }

    @GetMapping("/bindings")
    public Ajax listBindings() {
        return Ajax.success(oauthBindingService.listForUser(requireUsername()));
    }

    @PostMapping("/bindings/{provider}/start")
    public Ajax startBind(@PathVariable String provider,
                          @RequestParam(value = "return_origin", required = false) String returnOrigin,
                          HttpServletRequest request) {
        OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
        String username = requireUsername();
        String url = oauthFacade.startAuthorize(providerId, OAuthPurpose.BIND, username, returnOrigin, request);
        return Ajax.success(Map.of("authorizeUrl", url));
    }

    @DeleteMapping("/bindings/{provider}")
    public Ajax unbind(@PathVariable String provider) {
        OAuthProviderId providerId = OAuthProviderId.fromPath(provider);
        oauthBindingService.unbind(requireUsername(), providerId);
        return Ajax.success();
    }

    private static String requireUsername() {
        String username = SecurityUtils.getCurrentUsername();
        if (!StringUtils.hasText(username)) {
            throw new BizException(401, "未登录");
        }
        return username;
    }
}
