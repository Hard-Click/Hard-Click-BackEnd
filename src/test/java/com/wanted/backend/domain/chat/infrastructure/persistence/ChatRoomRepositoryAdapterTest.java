package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import(ChatRoomRepositoryAdapter.class)
class ChatRoomRepositoryAdapterTest {

    @Autowired
    private ChatRoomRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("이미 저장된 채팅방을 다시 save()하면 새 행이 아니라 기존 행이 갱신된다")
    void save_onExistingChatRoom_updatesInPlace_notInsertsNewRow() {
        // given
        ChatRoom created = ChatRoom.create(100L, 1L);
        ChatRoom saved = adapter.save(created);
        em.flush();
        em.clear();

        ChatRoom loaded = adapter.findById(saved.getId()).orElseThrow();
        ChatRoom closed = ChatRoom.restore(loaded.getId(), loaded.getStudyId(), loaded.getHostId(),
                ChatRoomStatus.CLOSED, loaded.getCreatedAt(), LocalDateTime.now());

        // when
        adapter.save(closed);
        em.flush();
        em.clear();

        // then
        long rowCount = em.getEntityManager()
                .createQuery("SELECT COUNT(c) FROM ChatRoomJpaEntity c", Long.class)
                .getSingleResult();
        assertThat(rowCount).isEqualTo(1);

        ChatRoom reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
    }
}
