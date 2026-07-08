package com.wanted.backend.domain.chat.infrastructure.study;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.domain.study.application.port.ChatRoomQueryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * study 도메인이 정의한 ChatRoomQueryPort의 구현체.
 * study는 이 클래스를 모른 채 인터페이스만 의존하고, 실제 조회는 chat 자신의 리포지토리로 처리한다.
 */
@Component
public class ChatRoomQueryPortAdapter implements ChatRoomQueryPort {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomQueryPortAdapter(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    @Override
    public Optional<Long> findChatRoomIdByStudyId(Long studyId) {
        return chatRoomRepository.findByStudyId(studyId).map(ChatRoom::getId);
    }
}
