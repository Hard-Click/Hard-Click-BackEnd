package com.wanted.backend.domain.chat.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomParticipantTest {

    @Test
    @DisplayName("아직 아무것도 읽지 않은 참여자가 메시지를 읽으면 lastReadMessageId가 설정된다")
    void markRead_success_firstRead() {
        ChatRoomParticipant participant = ChatRoomParticipant.restore(1L, 100L, 1L, LocalDateTime.now(), null);

        participant.markRead(50L);

        assertThat(participant.getLastReadMessageId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("더 최신 메시지를 읽으면 lastReadMessageId가 전진한다")
    void markRead_success_advances() {
        ChatRoomParticipant participant = ChatRoomParticipant.restore(1L, 100L, 1L, LocalDateTime.now(), 30L);

        participant.markRead(50L);

        assertThat(participant.getLastReadMessageId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("이미 읽은 것보다 과거 메시지 ID로는 역행하지 않는다")
    void markRead_doesNotRegress() {
        ChatRoomParticipant participant = ChatRoomParticipant.restore(1L, 100L, 1L, LocalDateTime.now(), 50L);

        participant.markRead(10L);

        assertThat(participant.getLastReadMessageId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("null 메시지 ID로 호출하면 아무 변화가 없다")
    void markRead_null_noop() {
        ChatRoomParticipant participant = ChatRoomParticipant.restore(1L, 100L, 1L, LocalDateTime.now(), 50L);

        participant.markRead(null);

        assertThat(participant.getLastReadMessageId()).isEqualTo(50L);
    }
}
