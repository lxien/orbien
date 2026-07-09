package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "file_share_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_file_share_proxy_id_username", columnNames = {"proxy_id", "username"}),
        indexes = @Index(name = "idx_file_share_username", columnList = "username"))
@NoArgsConstructor
public class FileShareUserDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proxy_id")
    private String proxyId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    /** read | read_write */
    @Column(name = "permission")
    private String permission;

    public FileShareUserDO(String proxyId, String username, String password, String permission) {
        this.proxyId = proxyId;
        this.username = username;
        this.password = password;
        this.permission = permission;
    }
}
