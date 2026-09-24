package com.graduate.graidaicommunity.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    /**
     * 读取application.properties配置文件里面的配置，赋值给成员变量
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-ms:86400000}")
    private long expireMs;

    private String secureSecret;
//@PostConstruct作用Bean完成依赖注入之后，Spring自动调用这个init（）方法
    @PostConstruct
    public void init() {
        //不为null,调用`trim()`**去掉首尾空格**，防止配置文件不小心多打空格
        String raw = (secret == null) ? "" : secret.trim();
        if (raw.isEmpty()) {
            log.error("JWT Secret 未配置，应用无法启动");
            throw new IllegalStateException("jwt.secret 必须配置且长度 >= 8");
        }
        if (raw.length() < 32) {
            log.warn("JWT 密钥长度 {} 不足 32 字节，自动循环补足", raw.length());
            StringBuilder sb = new StringBuilder(raw);
            while (sb.length() < 32) {
                sb.append(raw);
            }
            this.secureSecret = sb.substring(0, 32);//
        } else {
            this.secureSecret = raw;
        }
        log.info("JWT 密钥初始化完成，有效长度：{}", secureSecret.length());
    }

    //生成token
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(SignatureAlgorithm.HS256, secureSecret)
                .compact();
    }
//验证token
    public Long parseToken(String token) {                  //检查签名对不对、过没过期、格式合不合法
        String userIdStr = Jwts.parser()
                .setSigningKey(secureSecret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return Long.parseLong(userIdStr);
    }

    //解析token
    /**
     * 安全解析token，失败返回null（适用于非强制鉴权场景）
     */
    public Long getUserId(String token) {
        try {
            String userId = Jwts.parser()
                    .setSigningKey(secureSecret)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            return Long.valueOf(userId);
        } catch (Exception e) {
            log.warn("解析token异常：{}", e.getMessage());
            return null;
        }
    }
    //校验token
    public boolean verifyToken(String token) {
        try {
            Jwts.parser().setSigningKey(secureSecret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("token校验失败：{}", e.getMessage());
            return false;
        }
    }
}