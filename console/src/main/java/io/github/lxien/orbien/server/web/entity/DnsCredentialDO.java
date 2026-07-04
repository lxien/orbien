package io.github.lxien.orbien.server.web.entity;

import io.github.lxien.orbien.server.web.entity.converter.DnsCredentialStatusConverter;
import io.github.lxien.orbien.server.web.entity.converter.DnsProviderTypeConverter;
import io.github.lxien.orbien.server.web.enums.DnsCredentialStatus;
import io.github.lxien.orbien.server.web.enums.DnsProviderType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dns_credential")
public class DnsCredentialDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = DnsProviderTypeConverter.class)
    @Column(nullable = false)
    private DnsProviderType provider;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Convert(converter = DnsCredentialStatusConverter.class)
    @Column(nullable = false)
    private DnsCredentialStatus status = DnsCredentialStatus.UNTESTED;

    @Column(name = "account_hint")
    private String accountHint;

    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;

    @Column(name = "last_test_message")
    private String lastTestMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
