package io.github.lxien.orbien.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileShareAuthConfig implements Serializable {

    private boolean enabled;
    private final List<FileShareUser> users = new CopyOnWriteArrayList<>();

    public boolean hasUsers() {
        return !users.isEmpty();
    }

    public void addUser(FileShareUser user) {
        if (user != null) {
            users.add(user);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileShareUser implements Serializable {
        private String username;
        private String password;
        /**
         * read | read_write
         */
        private String permission;
    }
}
