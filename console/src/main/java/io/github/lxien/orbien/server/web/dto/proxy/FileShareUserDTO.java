package io.github.lxien.orbien.server.web.dto.proxy;

import lombok.Data;

@Data
public class FileShareUserDTO {
    private Long id;
    private String username;
    private String permission;
}
