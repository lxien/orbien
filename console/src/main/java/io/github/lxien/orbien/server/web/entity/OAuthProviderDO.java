package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oauth_provider",
        uniqueConstraints = @UniqueConstraint(name = "uk_oauth_provider", columnNames = "provider"))
public class OAuthProviderDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret_enc", columnDefinition = "TEXT")
    private String clientSecretEnc;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
