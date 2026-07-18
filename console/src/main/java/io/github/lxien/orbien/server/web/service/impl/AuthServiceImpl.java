package io.github.lxien.orbien.server.web.service.impl;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.auth.LoginDTO;
import io.github.lxien.orbien.server.web.param.auth.LoginParam;
import io.github.lxien.orbien.server.web.repository.UserRepository;
import io.github.lxien.orbien.server.web.security.TokenUtil;
import io.github.lxien.orbien.server.web.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserRepository userRepository;
    @Override
    public LoginDTO login(LoginParam param) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(param.getUserName(), param.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return issueToken(param.getUserName());
    }

    @Override
    public LoginDTO issueToken(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BizException("用户名不能为空");
        }
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户不存在"));
        return new LoginDTO(tokenUtil.generateToken(username));
    }
}
