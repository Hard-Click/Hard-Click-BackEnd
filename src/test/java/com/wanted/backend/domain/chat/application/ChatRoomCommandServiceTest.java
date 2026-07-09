package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.service.ChatRoomCommandService;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomCommandServiceTest {

    @InjectMocks
    private ChatRoomCommandService chatRoomCommandService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Test
    @DisplayName("채팅방 생성 시 방장이 첫 참여자로 등록된다")
    void createRoom_success() {
        // given
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation -> {
                    ChatRoom arg = invocation.getArgument(0);
                    return ChatRoom.restore(200L, arg.getStudyId(), arg.getHostId(),
                            arg.getStatus(), arg.getCreatedAt(), arg.getUpdatedAt());
                });

        // when
        Long chatRoomId = chatRoomCommandService.createRoom(100L, 1L);

        // then
        assertThat(chatRoomId).isEqualTo(200L);

        ArgumentCaptor<ChatRoomParticipant> captor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
        verify(chatRoomParticipantRepository).save(captor.capture());
        assertThat(captor.getValue().getChatRoomId()).isEqualTo(200L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("채팅방 저장에 실패하면 예외가 전파되고 참여자 등록은 시도되지 않는다")
    void createRoom_chatRoomSaveFails_throwsException() {
        // given
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() -> chatRoomCommandService.createRoom(100L, 1L))
                .isInstanceOf(RuntimeException.class);
        verify(chatRoomParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여자 저장에 실패하면 예외가 전파된다")
    void createRoom_participantSaveFails_throwsException() {
        // given
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation -> {
                    ChatRoom arg = invocation.getArgument(0);
                    return ChatRoom.restore(200L, arg.getStudyId(), arg.getHostId(),
                            arg.getStatus(), arg.getCreatedAt(), arg.getUpdatedAt());
                });
        willThrow(new RuntimeException("DB error"))
                .given(chatRoomParticipantRepository).save(any(ChatRoomParticipant.class));

        // when & then
        assertThatThrownBy(() -> chatRoomCommandService.createRoom(100L, 1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("참여자를 추가하면 채팅방 참여자로 저장된다")
    void addParticipant_success() {
        // when
        chatRoomCommandService.addParticipant(200L, 3L);

        // then
        ArgumentCaptor<ChatRoomParticipant> captor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
        verify(chatRoomParticipantRepository).save(captor.capture());
        assertThat(captor.getValue().getChatRoomId()).isEqualTo(200L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("채팅방을 닫으면 CLOSED 상태로 저장된다")
    void closeRoom_success() {
        // given
        ChatRoom activeRoom = ChatRoom.restore(200L, 100L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        given(chatRoomRepository.findById(200L)).willReturn(Optional.of(activeRoom));
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        chatRoomCommandService.closeRoom(200L);

        // then
        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 닫으려 하면 예외가 발생한다")
    void closeRoom_fail_notFound() {
        // given
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomCommandService.closeRoom(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());

        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여자를 제거하면 채팅방 참여자 목록에서 삭제된다")
    void removeParticipant_success() {
        // when
        chatRoomCommandService.removeParticipant(200L, 3L);

        // then
        verify(chatRoomParticipantRepository).deleteByChatRoomIdAndMemberId(200L, 3L);
    }
}
