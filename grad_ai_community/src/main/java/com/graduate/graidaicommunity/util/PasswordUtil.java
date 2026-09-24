package com.graduate.graidaicommunity.util;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {
//加密
    public String encrypt(String rawPassword) {//随机生成一把 "盐"，默认 cost=10（表示内部做 2¹⁰ 轮哈希，故意算慢）

        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());//hashpw（明文，盐）
    }
//验证
    public boolean match(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}