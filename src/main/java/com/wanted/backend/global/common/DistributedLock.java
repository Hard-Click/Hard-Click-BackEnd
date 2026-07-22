package com.wanted.backend.global.common;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis SETNX 기반 분산 락. 락 구간을 콜백으로 감싸 획득→실행→해제를 한 곳에서 처리한다.
 *
 * - 락 획득 실패 시 {@link ErrorCode#DUPLICATE_PAYMENT_REQUEST}(동일 자원에 대한 중복 요청).
 * - 소유 토큰(랜덤 UUID)을 Lua로 비교해 자기 락만 해제한다(TTL 만료 후 타인이 잡은 락을 지우지 않도록).
 * - 해제 실패는 로그만 남기고 삼켜, 원 작업 결과에 영향을 주지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public <T> T runWithLock(String key, Duration ttl, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (acquired == null || !acquired) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        try {
            return action.get();
        } finally {
            try {
                redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
            } catch (RuntimeException e) {
                log.error("[LOCK_RELEASE_FAILED] key: {}", key, e);
            }
        }
    }

    public void runWithLock(String key, Duration ttl, Runnable action) {
        runWithLock(key, ttl, () -> {
            action.run();
            return null;
        });
    }
}
