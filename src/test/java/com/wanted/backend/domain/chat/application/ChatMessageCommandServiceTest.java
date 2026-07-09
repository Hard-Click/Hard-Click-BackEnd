package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.event.ChatMessagePersistedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
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
    private ApplicationEventPublisher eventPublisher;

    private ChatRoom activeChatRoom() {
        return ChatRoom.restore(45L, 100L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    private ChatRoom closedChatRoom() {
        return ChatRoom.restore(45L, 100L, 1L, ChatRoomStatus.CLOSED, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("참여자가 메시지를 보내면 저장 후 ChatMessagePersistedEvent가 발행된다")
    void send_success() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage arg = invocation.getArgument(0);
            return ChatMessage.restore(500L, arg.getChatRoomId(), arg.getSenderId(), arg.getContent(), arg.getSentAt());
        });

        // when
        chatMessageCommandService.send(new SendMessageCommand(45L, 1L, "안녕하세요"));

        // then
        ArgumentCaptor<ChatMessagePersistedEvent> captor = ArgumentCaptor.forClass(ChatMessagePersistedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ChatMessagePersistedEvent event = captor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(45L);
        assertThat(event.messageId()).isEqualTo(500L);
        assertThat(event.senderId()).isEqualTo(1L);
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
        verify(eventPublisher, never()).publishEvent(any());
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
        verify(eventPublisher, never()).publishEvent(any());
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
        verify(eventPublisher, never()).publishEvent(any());
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("내용이 1000자를 초과하면 예외가 발생한다")
    void send_fail_contentTooLong() {
        // given
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatMessageCommandService.send(new SendMessageCommand(45L, 1L, "a".repeat(1001))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG.getMessage());

        verify(chatMessageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
