package io.github.lxien.orbien.server.web.security;
import com.google.gson.Gson;
import io.github.lxien.orbien.server.web.common.message.Ajax;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
/**
 * 自定义访问拒绝处理器，用于处理 403 未授权错误
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json; charset=utf-8");
        Ajax error = Ajax.error(403, "权限不足，无法访问该资源");
        String json = new Gson().toJson(error);
        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
}
