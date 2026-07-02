package com.xiaoniucode.etp.server.web.dto.proxy;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpsProxyDetailDTO extends HttpProxyDetailDTO {
    private Boolean forceHttps;
}
