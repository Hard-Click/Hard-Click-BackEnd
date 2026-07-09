package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatMessageType;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemLeaveMessage;
import com.wanted.backend.domain.study.application.event.StudyLeftEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyLeftBroadcastListenerTest {

    @InjectMocks
    private StudyLeftBroadcastListener listener;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatParticipantPresenceResolver presenceResolver;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("퇴장 이벤트를 받으면 퇴장 메시지와 갱신된 참여자 목록을 함께 브로드캐스트한다")
    void handle_success() {
        // given
        StudyLeftEvent event = new StudyLeftEvent(12L, 45L, 2L, 2);
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(2L, "김민수"));
        List<ParticipantPresenceMessage> participants = List.of(new ParticipantPresenceMessage(1L, "이*연", true));
        given(presenceResolver.resolve(12L)).willReturn(participants);

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<SystemLeaveMessage> captor = ArgumentCaptor.forClass(SystemLeaveMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/12"), captor.capture());
        SystemLeaveMessage message = captor.getValue();
        assertThat(message.type()).isEqualTo("SYSTEM_LEAVE");
        assertThat(message.message()).isEqualTo("김*수님이 퇴장했습니다");
        assertThat(message.participantCount()).isEqualTo(1);
        assertThat(message.participants()).isEqualTo(participants);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getChatRoomId()).isEqualTo(12L);
        assertThat(messageCaptor.getValue().getSenderId()).isNull();
        assertThat(messageCaptor.getValue().getType()).isEqualTo(ChatMessageType.SYSTEM_LEAVE);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("김*수님이 퇴장했습니다");
    }

    @Test
    @DisplayName("이름 조회가 실패해도 알 수 없음으로 대체되어 브로드캐스트는 그대로 수행된다")
    void handle_success_nameResolutionFails() {
        // given
        StudyLeftEvent event = new StudyLeftEvent(12L, 45L, 2L, 1);
        given(memberNamePort.getNamesByMemberIds(any())).willThrow(new RuntimeException("timeout"));
        given(presenceResolver.resolve(12L)).willReturn(List.of());

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<SystemLeaveMessage> captor = ArgumentCaptor.forClass(SystemLeaveMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/12"), captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("알 수 없음님이 퇴장했습니다");
    }
}
