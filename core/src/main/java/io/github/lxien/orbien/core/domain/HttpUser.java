package io.github.lxien.orbien.core.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class HttpUser {
    private String username;
    private String password;

    public HttpUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static HttpUser of(String username, String password) {
        return new HttpUser(username, password);
    }
}
