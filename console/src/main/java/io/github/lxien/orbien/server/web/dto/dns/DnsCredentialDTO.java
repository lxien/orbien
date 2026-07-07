package io.github.lxien.orbien.server.web.dto.dns;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DnsCredentialDTO implements Serializable {
    private Long id;
    private String name;
    private Integer provider;
    private String providerLabel;
    private Integer status;
    private String accountHint;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime lastTestAt;
    private String lastTestMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
