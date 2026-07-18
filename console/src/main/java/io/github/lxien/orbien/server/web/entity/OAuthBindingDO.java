package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oauth_binding",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_binding_external", columnNames = {"provider", "external_id"}),
                @UniqueConstraint(name = "uk_oauth_binding_user", columnNames = {"username", "provider"})
        })
public class OAuthBindingDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "external_login", length = 255)
    private String externalLogin;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @CreationTimestamp
    @Column(name = "bound_at", nullable = false)
    private LocalDateTime boundAt;
}
