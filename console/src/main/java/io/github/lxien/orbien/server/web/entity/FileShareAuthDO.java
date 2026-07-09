package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "file_share_auth")
@NoArgsConstructor
@AllArgsConstructor
public class FileShareAuthDO {
    @Id
    @Column(name = "proxy_id")
    private String proxyId;

    @Column(name = "enabled")
    private Boolean enabled;
}
