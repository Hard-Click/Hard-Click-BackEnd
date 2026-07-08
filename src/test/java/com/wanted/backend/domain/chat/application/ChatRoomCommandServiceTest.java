package com.wanted.backend.domain.chat.application;

import com.wanted.backend.domain.chat.application.service.ChatRoomCommandService;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
