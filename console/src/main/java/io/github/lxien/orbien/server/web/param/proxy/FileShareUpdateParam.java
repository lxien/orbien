package io.github.lxien.orbien.server.web.param.proxy;

import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FileShareUpdateParam {
    @NotEmpty(message = "id 不能为空")
    private String id;
    @NotEmpty(message = "name 不能为空")
    private String name;
    @NotNull(message = "domainType 不能为空")
    @EnumValue(enumClass = DomainType.class)
    private Integer domainType;
    private List<String> customDomains;
    private List<SubdomainBindingParam> subdomainBindings;
    @NotEmpty(message = "根目录不能为空")
    private String rootPath;
    @Min(value = 1, message = "总带宽限制必须大于0")
    private Integer limitTotal;
    private Boolean authEnabled;
    private List<FileShareAuthUserParam> authUsers = new ArrayList<>();
    private Long maxUploadSize;
    private Boolean allowUpload;
    private Boolean allowDelete;
    private Boolean allowMkdir;
}
