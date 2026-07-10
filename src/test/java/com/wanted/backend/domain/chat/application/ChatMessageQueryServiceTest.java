package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.result.ChatMessageDetail;
import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;
import com.wanted.backend.domain.chat.application.service.ChatMessageQueryService;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatMessageType;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatMessageQueryServiceTest {

    @InjectMocks
    private ChatMessageQueryService chatMessageQueryService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberNamePort memberNamePort;

    private ChatRoom activeRoom() {
        return ChatRoom.restore(12L, 45L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("참여자가 조회하면 오래된순으로 정렬된 메시지 목록과 다음 커서가 함께 반환된다")
    void getMessages_success() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);

        // 저장소는 최신순(DESC)으로 size+1개를 반환한다고 가정 (301이 가장 최신)
        ChatMessage msg301 = ChatMessage.restore(301L, 12L, 2L, ChatMessageType.CHAT,
                "안녕하세요!", LocalDateTime.of(2026, 7, 7, 21, 0));
        ChatMessage msg300 = ChatMessage.restore(300L, 12L, null, ChatMessageType.SYSTEM_JOIN,
                "김*민님이 입장했습니다", LocalDateTime.of(2026, 7, 7, 20, 59));
        given(chatMessageRepository.findByChatRoomIdBeforeCursor(12L, null, 3))
                .willReturn(List.of(msg301, msg300));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(2L, "김민수"));

        // when
        ChatMessageListResult result = chatMessageQueryService.getMessages(12L, null, 2, 1L);

        // then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).messageId()).isEqualTo(300L);
        assertThat(result.messages().get(0).type()).isEqualTo("SYSTEM_JOIN");
        assertThat(result.messages().get(0).senderId()).isNull();
        assertThat(result.messages().get(0).senderName()).isNull();
        assertThat(result.messages().get(1).messageId()).isEqualTo(301L);
        assertThat(result.messages().get(1).type()).isEqualTo("CHAT");
        assertThat(result.messages().get(1).senderId()).isEqualTo(2L);
        assertThat(result.messages().get(1).senderName()).isEqualTo("김*수");
        assertThat(result.nextCursorId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("조회된 메시지가 size보다 많으면 hasNext가 true이고 마지막 한 건은 잘려나간다")
    void getMessages_success_hasNext() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);

        ChatMessage msg3 = ChatMessage.restore(303L, 12L, 2L, ChatMessageType.CHAT,
                "c", LocalDateTime.of(2026, 7, 7, 21, 2));
        ChatMessage msg2 = ChatMessage.restore(302L, 12L, 2L, ChatMessageType.CHAT,
                "b", LocalDateTime.of(2026, 7, 7, 21, 1));
        ChatMessage msg1 = ChatMessage.restore(301L, 12L, 2L, ChatMessageType.CHAT,
                "a", LocalDateTime.of(2026, 7, 7, 21, 0));
        given(chatMessageRepository.findByChatRoomIdBeforeCursor(12L, null, 3))
                .willReturn(List.of(msg3, msg2, msg1));
        given(memberNamePort.getNamesByMemberIds(anyCollection())).willReturn(Map.of(2L, "김민수"));

        // when
        ChatMessageListResult result = chatMessageQueryService.getMessages(12L, null, 2, 1L);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).messageId()).isEqualTo(302L);
        assertThat(result.messages().get(1).messageId()).isEqualTo(303L);
        assertThat(result.nextCursorId()).isEqualTo(302L);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 조회하면 예외가 발생한다")
    void getMessages_fail_notFound() {
        // given
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageQueryService.getMessages(999L, null, 20, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 예외가 발생한다")
    void getMessages_fail_forbidden() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatMessageQueryService.getMessages(12L, null, 20, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("메시지가 없으면 빈 목록과 hasNext=false, nextCursorId=null을 반환한다")
    void getMessages_success_empty() {
        // given
        given(chatRoomRepository.findById(12L)).willReturn(Optional.of(activeRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(12L, 1L)).willReturn(true);
        given(chatMessageRepository.findByChatRoomIdBeforeCursor(12L, null, 21)).willReturn(List.of());

        // when
        ChatMessageListResult result = chatMessageQueryService.getMessages(12L, null, 20, 1L);

        // then
        assertThat(result.messages()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursorId()).isNull();
    }
}
