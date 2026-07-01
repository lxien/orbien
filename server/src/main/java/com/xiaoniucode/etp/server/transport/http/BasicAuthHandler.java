package com.xiaoniucode.etp.server.transport.http;

import com.xiaoniucode.etp.core.domain.HttpUser;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.core.domain.BasicAuthConfig;
import com.xiaoniucode.etp.core.utils.ChannelUtils;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.utils.NettyHttpUtils;
import com.xiaoniucode.etp.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;

@Component
@ChannelHandler.Sharable
public class BasicAuthHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(BasicAuthHandler.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DomainRegistry domainRegistry;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            String basicAuthHeader = visitor.attr(AttributeKeys.BASIC_AUTH_HEADER).get();
            BasicAuthConfig basicAuth = config.getBasicAuth();
            if (basicAuth != null && basicAuth.isEnabled()) {
                if (basicAuthHeader == null || !basicAuthHeader.toLowerCase().startsWith("basic ")) {
                    sendBasicAuth(visitor);
                    return;
                }
                try {
                    String base64Credentials = basicAuthHeader.substring(6).trim();
                    String credentials = new String(Base64.getDecoder().decode(base64Credentials), CharsetUtil.UTF_8);
                    String[] parts = credentials.split(":", 2);
                    if (parts.length == 2) {
                        String username = parts[0];
                        String password = parts[1];
                        if (!check(username, password, basicAuth)) {
                            sendBasicAuth(visitor);
                            return;
                        }
                    } else {
                        sendBasicAuth(visitor);
                        return;
                    }
                } catch (Exception e) {
                    logger.debug("Basic Auth 解码失败: {}", e.getMessage());
                    sendBasicAuth(visitor);
                    return;
                }
            }
        }
        ctx.fireChannelRead(msg);
    }

    private void sendBasicAuth(Channel visitor) {
        NettyHttpUtils.sendBasicAuth(visitor).addListener(future -> {
            ChannelUtils.closeOnFlush(visitor);
        });
    }

    private boolean check(String username, String password, BasicAuthConfig basicAuth) {
        HttpUser user = basicAuth.getUser(username);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }

}
