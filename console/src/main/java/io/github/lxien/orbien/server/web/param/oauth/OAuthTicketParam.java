package io.github.lxien.orbien.server.web.param.oauth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthTicketParam {
    @NotBlank
    private String ticket;
}
