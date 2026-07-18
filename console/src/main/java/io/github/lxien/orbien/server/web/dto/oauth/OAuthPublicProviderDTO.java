package io.github.lxien.orbien.server.web.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthPublicProviderDTO {
    private String provider;
    private String displayName;
}
