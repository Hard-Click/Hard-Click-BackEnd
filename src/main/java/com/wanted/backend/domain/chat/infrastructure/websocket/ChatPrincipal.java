package com.wanted.backend.domain.chat.infrastructure.websocket;

import java.security.Principal;

public class ChatPrincipal implements Principal {

    private final Long memberId;

    public ChatPrincipal(Long memberId) {
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
