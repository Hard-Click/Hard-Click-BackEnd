package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;
import com.wanted.backend.domain.chat.application.service.SocketTicketCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocketTicketCommandServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SocketTicketCommandService service;

    @BeforeEach
    void setUp() {
        service = new SocketTicketCommandService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("발급 시 UUID 형식의 티켓이 30초 TTL로 Redis에 저장된다")
    void issue_success() {
        // when
        SocketTicketResult result = service.issue(1L);

        // then
        assertThat(UUID.fromString(result.ticket())).isNotNull();
        assertThat(result.expiresInSeconds()).isEqualTo(30);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(30)));
        assertThat(keyCaptor.getValue()).contains(result.ticket());
        assertThat(valueCaptor.getValue()).isEqualTo("1");
    }

    @Test
    @DisplayName("호출할 때마다 서로 다른 티켓이 발급된다")
    void issue_success_uniqueTickets() {
        // when
        SocketTicketResult first = service.issue(1L);
        SocketTicketResult second = service.issue(1L);

        // then
        assertThat(first.ticket()).isNotEqualTo(second.ticket());
    }
}
