package io.github.lxien.orbien.core.domain;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TransportCustomConfig {
    private Boolean multiplex;
    private Boolean encrypt;
    private Boolean compress;
}
