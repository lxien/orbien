package io.github.lxien.orbien.server.web.param.proxy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileShareAuthUserParam {
    private Long id;
    private String username;
    private String password;
    /** read | read_write */
    private String permission;
}
