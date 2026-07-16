package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import(ChatRoomParticipantRepositoryAdapter.class)
class ChatRoomParticipantRepositoryAdapterTest {

    @Autowired
    private ChatRoomParticipantRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("처음 읽으면 lastReadMessageId가 설정된다")
    void updateLastReadMessageId_firstRead() {
        ChatRoomParticipant saved = adapter.save(ChatRoomParticipant.create(100L, 1L));
        em.flush();
        em.clear();

        adapter.updateLastReadMessageId(100L, 1L, 50L);
        em.flush();
        em.clear();

        ChatRoomParticipantJpaEntity found = em.find(ChatRoomParticipantJpaEntity.class, saved.getId());
        assertThat(found.getLastReadMessageId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("과거 메시지 ID로 갱신을 시도해도 DB 레벨에서 역행하지 않는다")
    void updateLastReadMessageId_doesNotRegress() {
        ChatRoomParticipant saved = adapter.save(ChatRoomParticipant.create(100L, 1L));
        em.flush();
        adapter.updateLastReadMessageId(100L, 1L, 50L);
        em.flush();
        em.clear();

        adapter.updateLastReadMessageId(100L, 1L, 10L);
        em.flush();
        em.clear();

        ChatRoomParticipantJpaEntity found = em.find(ChatRoomParticipantJpaEntity.class, saved.getId());
        assertThat(found.getLastReadMessageId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("다른 참여자의 읽음 상태에는 영향을 주지 않는다")
    void updateLastReadMessageId_scopedToParticipant() {
        ChatRoomParticipant me = adapter.save(ChatRoomParticipant.create(100L, 1L));
        ChatRoomParticipant other = adapter.save(ChatRoomParticipant.create(100L, 2L));
        em.flush();
        em.clear();

        adapter.updateLastReadMessageId(100L, 1L, 50L);
        em.flush();
        em.clear();

        assertThat(em.find(ChatRoomParticipantJpaEntity.class, me.getId()).getLastReadMessageId()).isEqualTo(50L);
        assertThat(em.find(ChatRoomParticipantJpaEntity.class, other.getId()).getLastReadMessageId()).isNull();
    }
}
