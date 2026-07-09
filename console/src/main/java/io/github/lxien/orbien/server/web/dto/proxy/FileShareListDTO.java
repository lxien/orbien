package io.github.lxien.orbien.server.web.dto.proxy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FileShareListDTO extends ProxyListDTO implements Serializable {
    private List<String> domains;
    private List<String> accessUrls;
    private String rootPath;
    private Boolean authEnabled;
    private Integer authUserCount;
    private Integer httpsProxyPort;
}
