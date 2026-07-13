/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.cli.credentials;

import com.moandjiezana.toml.Toml;
import io.github.lxien.orbien.common.utils.TomlUtils;
import io.github.lxien.orbien.core.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CredentialsStore {

    public static final int DEFAULT_SERVER_PORT = 9527;
    private static final Path CREDENTIALS_DIR = Paths.get(System.getProperty("user.home"), ".orbien");
    private static final Path CREDENTIALS_FILE = CREDENTIALS_DIR.resolve("credentials.toml");

    private CredentialsStore() {
    }

    public static Path credentialsPath() {
        return CREDENTIALS_FILE;
    }

    public static Optional<Credentials> load() {
        if (!Files.exists(CREDENTIALS_FILE)) {
            return Optional.empty();
        }
        Toml toml = TomlUtils.readToml(CREDENTIALS_FILE.toString());
        String serverAddr = toml.getString("server_addr");
        Long serverPort = toml.getLong("server_port");
        String token = toml.getString("token");
        if (!StringUtils.hasText(serverAddr) || !StringUtils.hasText(token)) {
            return Optional.empty();
        }
        Credentials credentials = new Credentials();
        credentials.setServerAddr(serverAddr.trim());
        credentials.setServerPort(serverPort != null ? serverPort.intValue() : DEFAULT_SERVER_PORT);
        credentials.setToken(token.trim());
        String loggedInAt = toml.getString("logged_in_at");
        if (StringUtils.hasText(loggedInAt)) {
            credentials.setLoggedInAt(loggedInAt.trim());
        }
        return Optional.of(credentials);
    }

    public static Credentials loadOrThrow() {
        return load().orElseThrow(() -> new IllegalArgumentException(
                "未配置凭据，运行: orbien login --server <host:port> --token <token>"));
    }

    public static void save(Credentials credentials) {
        validate(credentials);
        Map<String, Object> map = new HashMap<>();
        map.put("server_addr", credentials.getServerAddr());
        map.put("server_port", credentials.getServerPort());
        map.put("token", credentials.getToken());
        map.put("logged_in_at", OffsetDateTime.now().toString());
        try {
            Files.createDirectories(CREDENTIALS_DIR);
            setOwnerOnlyDirPermissions(CREDENTIALS_DIR);
            TomlUtils.write(map, CREDENTIALS_FILE.toString());
            setOwnerOnlyFilePermissions(CREDENTIALS_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("保存凭据失败: " + CREDENTIALS_FILE, e);
        }
    }

    public static void delete() {
        try {
            Files.deleteIfExists(CREDENTIALS_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("删除凭据失败: " + CREDENTIALS_FILE, e);
        }
    }

    public static void validate(Credentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("凭据不能为空");
        }
        if (!StringUtils.hasText(credentials.getServerAddr())) {
            throw new IllegalArgumentException("服务端地址不能为空");
        }
        if (!StringUtils.hasText(credentials.getToken())) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }
        validatePort(credentials.getServerPort());
    }

    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在 1-65535 范围内: " + port);
        }
    }

    private static void setOwnerOnlyDirPermissions(Path path) {
        if (isWindows()) {
            return;
        }
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException ignored) {
            // 权限设置失败不影响功能
        }
    }

    private static void setOwnerOnlyFilePermissions(Path path) {
        if (isWindows()) {
            return;
        }
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException ignored) {
            // 权限设置失败不影响功能
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
