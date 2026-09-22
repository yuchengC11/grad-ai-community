package com.graduate.graidaicommunity.util;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {
//加密
    public String encrypt(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }
//验证
    public boolean match(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}