package io.github.lxien.orbien.server.web.dto.dns;

import lombok.Data;

import java.io.Serializable;

@Data
public class DnsProviderFieldDTO implements Serializable {
    private String key;
    private String label;
    private String type;
    private Boolean required;
    private Boolean secret;
}
