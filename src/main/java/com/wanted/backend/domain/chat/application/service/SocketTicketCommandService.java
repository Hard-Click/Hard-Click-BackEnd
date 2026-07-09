package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;
import com.wanted.backend.domain.chat.application.usecase.SocketTicketCommandUseCase;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class SocketTicketCommandService implements SocketTicketCommandUseCase {

    private static final String TICKET_KEY_PREFIX = "chat:{socket-ticket}:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public SocketTicketCommandService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SocketTicketResult issue(Long memberId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(ticketKey(ticket), String.valueOf(memberId), TICKET_TTL);
        return new SocketTicketResult(ticket, (int) TICKET_TTL.toSeconds());
    }

    @Override
    public Optional<Long> consume(String ticket) {
        String memberId = redisTemplate.opsForValue().getAndDelete(ticketKey(ticket));
        if (memberId == null) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(memberId));
    }

    private String ticketKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }
}
