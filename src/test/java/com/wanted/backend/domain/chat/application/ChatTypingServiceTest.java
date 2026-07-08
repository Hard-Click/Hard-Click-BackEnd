package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.event.TypingEvent;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.service.ChatTypingService;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatTypingServiceTest {

    @InjectMocks
    private ChatTypingService chatTypingService;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("참여자가 타이핑하면 저장 없이 바로 브로드캐스트된다")
    void notifyTyping_success() {
        // given
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연"));

        // when
        chatTypingService.notifyTyping(45L, 1L);

        // then
        ArgumentCaptor<TypingEvent> captor = ArgumentCaptor.forClass(TypingEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/45"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("TYPING");
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
        assertThat(captor.getValue().name()).isEqualTo("이*연");
    }

    @Test
    @DisplayName("참여자가 아니면 예외가 발생하고 브로드캐스트되지 않는다")
    void notifyTyping_fail_notParticipant() {
        // given
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatTypingService.notifyTyping(45L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_FORBIDDEN.getMessage());

        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), any(Object.class));
    }
}
