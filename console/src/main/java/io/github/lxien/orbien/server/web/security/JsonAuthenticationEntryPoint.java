package io.github.lxien.orbien.server.web.security;
import com.google.gson.Gson;
import io.github.lxien.orbien.server.web.common.message.Ajax;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
/**
 * 自定义认证入口点，用于处理未授权错误并返回
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");
        Ajax error = Ajax.error(401, "未授权，请先登录");
        String json = new Gson().toJson(error);
        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
}
