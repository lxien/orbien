package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "socks5_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_socks5_proxy_id_username", columnNames = {"proxy_id", "username"}),
        indexes = @Index(name = "idx_socks5_username", columnList = "username"))
@NoArgsConstructor
public class Socks5UserDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proxy_id")
    private String proxyId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    public Socks5UserDO(String proxyId, String username, String password) {
        this.proxyId = proxyId;
        this.username = username;
        this.password = password;
    }
}
