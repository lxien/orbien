package io.github.lxien.orbien.server.web.security;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.common.utils.JsonUtils;
import io.github.lxien.orbien.server.web.config.ConsoleProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * 预览模式下拦截控制台写操作
 */
@Component
public class PreviewModeFilter extends OncePerRequestFilter {
    private static final String MESSAGE = "演示模式禁止操作";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name()
    );
    /**
     * 预览模式下允许的POST类路径
     */
    private static final List<String> ALLOWED_MUTATING_PATHS = List.of(
            "/api/auth/login",
            "/api/metrics/proxy/24h",
            "/api/cert-binding/preview"
    );

    private final ConsoleProperties consoleProperties;

    public PreviewModeFilter(ConsoleProperties consoleProperties) {
        this.consoleProperties = consoleProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!consoleProperties.isPreviewMode()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        if (path == null || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        if (method != null && SAFE_METHODS.contains(method.toUpperCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedMutatingPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        reject(response);
    }

    private static boolean isAllowedMutatingPath(String path) {
        for (String pattern : ALLOWED_MUTATING_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJson(Ajax.error(MESSAGE)));
        response.getWriter().flush();
    }
}
