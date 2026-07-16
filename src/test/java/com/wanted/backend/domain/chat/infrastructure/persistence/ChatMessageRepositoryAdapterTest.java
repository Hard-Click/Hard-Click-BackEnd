package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import({ChatMessageRepositoryAdapter.class, ChatRoomParticipantRepositoryAdapter.class})
class ChatMessageRepositoryAdapterTest {

    @Autowired
    private ChatMessageRepositoryAdapter adapter;

    @Autowired
    private ChatRoomParticipantRepositoryAdapter participantAdapter;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("커서 없이 조회하면 최신 메시지부터 최대 limit개를 최신순으로 반환한다")
    void findByChatRoomIdBeforeCursor_firstPage() {
        // given
        saveFiveMessages();
        em.flush();
        em.clear();

        // when
        List<ChatMessage> page = adapter.findByChatRoomIdBeforeCursor(100L, null, 3);

        // then
        assertThat(page).hasSize(3);
        assertThat(page).extracting(ChatMessage::getContent)
                .containsExactly("msg5", "msg4", "msg3");
    }

    @Test
    @DisplayName("커서를 지정하면 해당 ID보다 오래된 메시지만 최신순으로 반환한다")
    void findByChatRoomIdBeforeCursor_nextPage() {
        // given
        List<ChatMessage> saved = saveFiveMessages();
        em.flush();
        em.clear();
        Long cursorId = saved.get(2).getId(); // msg3

        // when
        List<ChatMessage> page = adapter.findByChatRoomIdBeforeCursor(100L, cursorId, 3);

        // then
        assertThat(page).hasSize(2);
        assertThat(page).extracting(ChatMessage::getContent)
                .containsExactly("msg2", "msg1");
    }

    @Test
    @DisplayName("다른 채팅방의 메시지는 섞이지 않는다")
    void findByChatRoomIdBeforeCursor_scopedToChatRoom() {
        // given
        adapter.save(ChatMessage.create(100L, 1L, "room100"));
        adapter.save(ChatMessage.create(200L, 1L, "room200"));
        em.flush();
        em.clear();

        // when
        List<ChatMessage> page = adapter.findByChatRoomIdBeforeCursor(100L, null, 10);

        // then
        assertThat(page).extracting(ChatMessage::getContent).containsExactly("room100");
    }

    @Test
    @DisplayName("한 번도 안 읽었으면 방의 전체 메시지 수가 unreadCount다")
    void countUnread_neverRead_countsAll() {
        participantAdapter.save(ChatRoomParticipant.create(100L, 1L));
        saveFiveMessages();
        em.flush();
        em.clear();

        long unread = adapter.countUnreadByChatRoomIdAndMemberId(100L, 1L);

        assertThat(unread).isEqualTo(5);
    }

    @Test
    @DisplayName("마지막으로 읽은 메시지 이후 것만 unreadCount로 잡힌다")
    void countUnread_afterLastRead_countsOnlyNewer() {
        participantAdapter.save(ChatRoomParticipant.create(100L, 1L));
        List<ChatMessage> saved = saveFiveMessages();
        em.flush();
        Long thirdMessageId = saved.get(2).getId(); // msg3
        participantAdapter.updateLastReadMessageId(100L, 1L, thirdMessageId);
        em.flush();
        em.clear();

        long unread = adapter.countUnreadByChatRoomIdAndMemberId(100L, 1L);

        assertThat(unread).isEqualTo(2); // msg4, msg5
    }

    @Test
    @DisplayName("최신 메시지까지 다 읽으면 unreadCount는 0이다")
    void countUnread_fullyRead_zero() {
        participantAdapter.save(ChatRoomParticipant.create(100L, 1L));
        List<ChatMessage> saved = saveFiveMessages();
        em.flush();
        Long lastMessageId = saved.get(4).getId(); // msg5
        participantAdapter.updateLastReadMessageId(100L, 1L, lastMessageId);
        em.flush();
        em.clear();

        long unread = adapter.countUnreadByChatRoomIdAndMemberId(100L, 1L);

        assertThat(unread).isZero();
    }

    @Test
    @DisplayName("다른 방의 메시지는 unreadCount에 섞이지 않는다")
    void countUnread_scopedToChatRoom() {
        participantAdapter.save(ChatRoomParticipant.create(100L, 1L));
        adapter.save(ChatMessage.create(100L, 2L, "room100"));
        adapter.save(ChatMessage.create(200L, 2L, "room200"));
        em.flush();
        em.clear();

        long unread = adapter.countUnreadByChatRoomIdAndMemberId(100L, 1L);

        assertThat(unread).isEqualTo(1);
    }

    private List<ChatMessage> saveFiveMessages() {
        return List.of(
                adapter.save(ChatMessage.create(100L, 1L, "msg1")),
                adapter.save(ChatMessage.create(100L, 1L, "msg2")),
                adapter.save(ChatMessage.create(100L, 1L, "msg3")),
                adapter.save(ChatMessage.create(100L, 1L, "msg4")),
                adapter.save(ChatMessage.create(100L, 1L, "msg5"))
        );
    }
}
