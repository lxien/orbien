package io.github.lxien.orbien.server.web.param.proxy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpsProxyUpdateParam extends HttpProxyUpdateParam {
    private Boolean forceHttps;
}
