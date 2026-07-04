package io.github.lxien.orbien.server.web.dto.dns;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DnsProviderSchemaDTO implements Serializable {
    private Integer provider;
    private String label;
    private List<DnsProviderFieldDTO> fields;
}
