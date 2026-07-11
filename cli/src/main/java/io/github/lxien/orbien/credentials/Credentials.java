package io.github.lxien.orbien.credentials;

import lombok.Data;

@Data
public class Credentials {
    private String serverAddr;
    private int serverPort;
    private String token;
    private String loggedInAt;
}
