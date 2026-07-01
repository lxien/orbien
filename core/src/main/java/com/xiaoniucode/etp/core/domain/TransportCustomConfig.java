package com.xiaoniucode.etp.core.domain;

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
