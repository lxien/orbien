package io.github.lxien.orbien.server.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "file_share_limits")
@NoArgsConstructor
public class FileShareLimitsDO {
    @Id
    @Column(name = "proxy_id")
    private String proxyId;

    @Column(name = "root_path")
    private String rootPath;

    @Column(name = "max_upload_size")
    private Long maxUploadSize;

    @Column(name = "allow_upload")
    private Boolean allowUpload;

    @Column(name = "allow_delete")
    private Boolean allowDelete;

    @Column(name = "allow_mkdir")
    private Boolean allowMkdir;

    @Column(name = "allow_move")
    private Boolean allowMove;

    @Column(name = "allow_rename")
    private Boolean allowRename;

    public FileShareLimitsDO(String proxyId) {
        this.proxyId = proxyId;
    }
}
