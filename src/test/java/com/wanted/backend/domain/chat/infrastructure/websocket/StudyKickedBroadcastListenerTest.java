package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemKickMessage;
import com.wanted.backend.domain.study.application.event.StudyKickedEvent;
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
class StudyKickedBroadcastListenerTest {

    @InjectMocks
    private StudyKickedBroadcastListener listener;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatParticipantPresenceResolver presenceResolver;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("강퇴 이벤트를 받으면 강퇴 메시지와 갱신된 참여자 목록을 함께 브로드캐스트한다")
    void handle_success() {
        // given
        StudyKickedEvent event = new StudyKickedEvent(12L, 45L, 2L, 2);
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(2L, "김민수"));
        List<ParticipantPresenceMessage> participants = List.of(new ParticipantPresenceMessage(1L, "이*연", true));
        given(presenceResolver.resolve(12L)).willReturn(participants);

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<SystemKickMessage> captor = ArgumentCaptor.forClass(SystemKickMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/12"), captor.capture());
        SystemKickMessage message = captor.getValue();
        assertThat(message.type()).isEqualTo("SYSTEM_KICK");
        assertThat(message.message()).isEqualTo("김*수님을 내보냈습니다");
        assertThat(message.kickedMemberId()).isEqualTo(2L);
        assertThat(message.participantCount()).isEqualTo(1);
        assertThat(message.participants()).isEqualTo(participants);
    }

    @Test
    @DisplayName("이름 조회가 실패해도 알 수 없음으로 대체되어 브로드캐스트는 그대로 수행된다")
    void handle_success_nameResolutionFails() {
        // given
        StudyKickedEvent event = new StudyKickedEvent(12L, 45L, 2L, 1);
        given(memberNamePort.getNamesByMemberIds(any())).willThrow(new RuntimeException("timeout"));
        given(presenceResolver.resolve(12L)).willReturn(List.of());

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<SystemKickMessage> captor = ArgumentCaptor.forClass(SystemKickMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/12"), captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("알 수 없음님을 내보냈습니다");
    }
}
