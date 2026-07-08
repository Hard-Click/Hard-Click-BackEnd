package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.service.ChatMessageCommandService;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageCommandServiceTest {

    @InjectMocks
    private ChatMessageCommandService chatMessageCommandService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatRoom activeChatRoom() {
        return ChatRoom.restore(45L, 100L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    private ChatRoom closedChatRoom() {
        return ChatRoom.restore(45L, 100L, 1L, ChatRoomStatus.CLOSED, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("참여자가 메시지를 보내면 저장 후 브로드캐스트된다")
    void send_success() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage arg = invocation.getArgument(0);
            return ChatMessage.restore(500L, arg.getChatRoomId(), arg.getSenderId(), arg.getContent(), arg.getSentAt());
        });
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연"));

        // when
        chatMessageCommandService.send(new SendMessageCommand(45L, 1L, "안녕하세요"));

        // then
        ArgumentCaptor<ChatMessageEvent> captor = ArgumentCaptor.forClass(ChatMessageEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/sub/chat-rooms/45"), captor.capture());
        ChatMessageEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("CHAT");
        assertThat(event.messageId()).isEqualTo(500L);
        assertThat(event.senderId()).isEqualTo(1L);
        assertThat(event.senderName()).isEqualTo("이*연");
        assertThat(event.content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("존재하지 않는 채팅방에 발행하면 예외가 발생한다")
    void send_fail_roomNotFound() {
        // given
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageCommandService.send(new SendMessageCommand(999L, 1L, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());

        verify(chatMessageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), any(Object.class));
    }

    @Test
    @DisplayName("참여자가 아니면 예외가 발생한다")
    void send_fail_notParticipant() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatMessageCommandService.send(new SendMessageCommand(45L, 999L, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_FORBIDDEN.getMessage());

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 종료된 채팅방에 발행하면 예외가 발생한다")
    void send_fail_roomClosed() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(closedChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatMessageCommandService.send(new SendMessageCommand(45L, 1L, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_CLOSED.getMessage());

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("내용이 비어 있으면 예외가 발생한다")
    void send_fail_blankContent() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatMessageCommandService.send(new SendMessageCommand(45L, 1L, "  ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED.getMessage());

        verify(chatMessageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), any(Object.class));
    }
}
