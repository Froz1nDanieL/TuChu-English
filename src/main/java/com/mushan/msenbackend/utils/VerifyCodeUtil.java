package com.mushan.msenbackend.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 */
@Component
public class VerifyCodeUtil {
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    private static final String REGISTER_CODE_PREFIX = "register:code:";
    private static final String RESET_CODE_PREFIX = "reset:code:";
    private static final long CODE_EXPIRE_TIME = 5; // 分钟
    private static final long SEND_INTERVAL = 60; // 秒
    
    /**
     * 生成6位数字验证码
     */
    public String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * 保存注册验证码
     */
    public void saveRegisterCode(String email, String code) {
        String key = REGISTER_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 保存重置密码验证码
     */
    public void saveResetCode(String email, String code) {
        String key = RESET_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    
    /**
     * 校验注册验证码
     */
    public boolean verifyRegisterCode(String email, String code) {
        String key = REGISTER_CODE_PREFIX + email;
        String savedCode = stringRedisTemplate.opsForValue().get(key);
        return code != null && code.equals(savedCode);
    }
    
    /**
     * 校验重置密码验证码
     */
    public boolean verifyResetCode(String email, String code) {
        String key = RESET_CODE_PREFIX + email;
        String savedCode = stringRedisTemplate.opsForValue().get(key);
        return code != null && code.equals(savedCode);
    }
    
    /**
     * 删除验证码
     */
    public void deleteCode(String email, String type) {
        String prefix = "register".equals(type) ? REGISTER_CODE_PREFIX : RESET_CODE_PREFIX;
        stringRedisTemplate.delete(prefix + email);
    }
    
    /**
     * 检查是否可以发送验证码（防止频繁发送）
     */
    public boolean canSendCode(String email, String type) {
        String prefix = "register".equals(type) ? REGISTER_CODE_PREFIX : RESET_CODE_PREFIX;
        String key = prefix + email;
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        // 如果剩余时间大于（有效期-发送间隔），说明刚发送过
        return expire == null || expire < (CODE_EXPIRE_TIME * 60 - SEND_INTERVAL);
    }
}
