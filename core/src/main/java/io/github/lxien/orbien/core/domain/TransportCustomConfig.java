package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TransportCustomConfig {
    private TransportProtocol protocol;
    private Boolean multiplex;
    private Boolean encrypt;
    private Boolean compress;
}
