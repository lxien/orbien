package io.github.lxien.orbien.server.web.security;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private PermitAll permitAll = new PermitAll();
    @Setter
    @Getter
    public static class PermitAll {
        private List<String> paths = new ArrayList<>();
    }
}