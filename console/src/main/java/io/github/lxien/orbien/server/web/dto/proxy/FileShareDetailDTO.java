package io.github.lxien.orbien.server.web.dto.proxy;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class FileShareDetailDTO {
    private String id;
    private String agentId;
    private String name;
    private Integer domainType;
    private List<String> customDomains;
    private List<SubdomainBindingDTO> subdomainBindings;
    private List<String> domains;
    private List<String> accessUrls;
    private String rootPath;
    private Long maxUploadSize;
    private Boolean allowUpload;
    private Boolean allowDelete;
    private Boolean allowMkdir;
    private Integer limitTotal;
    private Boolean authEnabled;
    private List<FileShareUserDTO> authUsers = new ArrayList<>();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
