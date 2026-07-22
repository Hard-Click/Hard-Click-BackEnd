package com.wanted.backend.global.common;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private DistributedLock distributedLock;

    private static final Duration TTL = Duration.ofSeconds(5);

    @Test
    @DisplayName("락 획득 시 콜백을 실행하고 결과를 반환한다")
    void runsActionWhenAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        String result = distributedLock.runWithLock("k", TTL, () -> "done");

        assertThat(result).isEqualTo("done");
    }

    @Test
    @DisplayName("락 획득 실패 시 DUPLICATE_PAYMENT_REQUEST를 던지고 콜백을 실행하지 않는다")
    void throwsWhenLockBusy() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThatThrownBy(() -> distributedLock.runWithLock("k", TTL, () -> ran.set(true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);

        assertThat(ran).isFalse();
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("락 해제 실패는 삼켜 원 작업 결과에 영향을 주지 않는다")
    void swallowsReleaseFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("del failed"));

        String result = distributedLock.runWithLock("k", TTL, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }
}
