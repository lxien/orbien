package io.github.lxien.orbien.server.web.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Component
public class TokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtil.class);
    private final String secret;
    private final long expiration;
    public TokenUtil(@Value("${jwt.secret:}") String secret,
                     @Value("${jwt.expiration:3600}") long expiration) {
        this.expiration = expiration;
        if (!StringUtils.hasText(secret)) {
            byte[] randomBytes = new byte[32];
            new SecureRandom().nextBytes(randomBytes);
            this.secret = Base64.getEncoder().encodeToString(randomBytes);
            logger.info("JWT密钥未配置，已自动生成32字节随机密钥");
        } else {
            this.secret = secret;
            logger.info("使用配置的 JWT 密钥");
        }
        logger.debug("Token令牌过期时间设置为: {}秒", expiration);
    }
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("created", now);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.debug("无效登陆令牌");
            throw e;
        }
    }
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            logger.debug("Token 令牌验证失败");
            return false;
        }
    }
}
