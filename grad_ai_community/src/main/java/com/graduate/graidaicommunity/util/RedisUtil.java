package com.graduate.graidaicommunity.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Long sAdd(String key, String... members) {
        return stringRedisTemplate.opsForSet().add(key, members);
    }

    public Long sRem(String key, String... members) {
        return stringRedisTemplate.opsForSet().remove(key, members);
    }

    public Boolean sIsMember(String key, String member) {
        return stringRedisTemplate.opsForSet().isMember(key, member);
    }

    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public Long sCard(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key, long delta) {
        return stringRedisTemplate.opsForValue().decrement(key, delta);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * Lua脚本原子切换 Set 成员（点赞/收藏通用）
     * @return 1=添加成功, 0=移除成功, null=执行失败需降级
     */
    public Long toggleSetAtomic(String key, String member) {
        String lua =
                "local exists = redis.call('sismember', KEYS[1], ARGV[1]); " +
                        "if exists == 1 then " +
                        "   redis.call('srem', KEYS[1], ARGV[1]); " +
                        "   return 0; " +
                        "else " +
                        "   redis.call('sadd', KEYS[1], ARGV[1]); " +
                        "   return 1; " +
                        "end";

        try {
            return stringRedisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(lua, Long.class),
                    java.util.Collections.singletonList(key),
                    member
            );
        } catch (Exception e) {
            log.error("Lua脚本执行失败: {}", e.getMessage());
            return null;
        }
    }

    public Long toggleLikeAtomic(String postLikeKey, String userId) {
        return toggleSetAtomic(postLikeKey, userId);
    }
}