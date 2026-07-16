package com.sms.nexus.common.util;

import com.sms.nexus.common.constant.AppConstant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    private final StringRedisTemplate redisTemplate;

    public IdGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String nextAdminId() {
        return AppConstant.ID_PREFIX_ADMIN + String.format("%06d", increment(AppConstant.REDIS_ID_COUNTER_PREFIX + "admin"));
    }

    public String nextStudentId() {
        return AppConstant.ID_PREFIX_STUDENT + String.format("%06d", increment(AppConstant.REDIS_ID_COUNTER_PREFIX + "student"));
    }

    public String nextTeacherId() {
        return AppConstant.ID_PREFIX_TEACHER + String.format("%06d", increment(AppConstant.REDIS_ID_COUNTER_PREFIX + "teacher"));
    }

    public String nextLogId() {
        return AppConstant.ID_PREFIX_LOG + String.format("%06d", increment(AppConstant.REDIS_ID_COUNTER_PREFIX + "log"));
    }

    public String nextNotificationId() {
        return AppConstant.ID_PREFIX_NOTIFICATION + String.format("%06d", increment(AppConstant.REDIS_ID_COUNTER_PREFIX + "notification"));
    }

    private long increment(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        // No expiration on counter keys to prevent ID collisions
        return value != null ? value : 1;
    }
}
