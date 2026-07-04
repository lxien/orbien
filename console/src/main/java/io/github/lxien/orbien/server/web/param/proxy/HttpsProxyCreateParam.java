package io.github.lxien.orbien.server.web.param.proxy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpsProxyCreateParam extends HttpProxyCreateParam {
    private Boolean forceHttps;
}
