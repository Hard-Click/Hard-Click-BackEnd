package com.wanted.backend.domain.chat.infrastructure.study;

import com.wanted.backend.domain.chat.application.usecase.ChatRoomCommandUseCase;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import org.springframework.stereotype.Component;

/**
 * study 도메인이 정의한 ChatRoomCommandPort의 구현체.
 * study는 이 클래스를 모른 채 인터페이스만 의존하고, 실제 처리는 chat 자신의 UseCase에 위임한다.
 */
@Component
public class ChatRoomCommandPortAdapter implements ChatRoomCommandPort {

    private final ChatRoomCommandUseCase chatRoomCommandUseCase;

    public ChatRoomCommandPortAdapter(ChatRoomCommandUseCase chatRoomCommandUseCase) {
        this.chatRoomCommandUseCase = chatRoomCommandUseCase;
    }

    @Override
    public Long createRoom(Long studyId, Long hostId) {
        return chatRoomCommandUseCase.createRoom(studyId, hostId);
    }

    @Override
    public void addParticipant(Long chatRoomId, Long memberId) {
        chatRoomCommandUseCase.addParticipant(chatRoomId, memberId);
    }

    @Override
    public void removeParticipant(Long chatRoomId, Long memberId) {
        chatRoomCommandUseCase.removeParticipant(chatRoomId, memberId);
    }
}
