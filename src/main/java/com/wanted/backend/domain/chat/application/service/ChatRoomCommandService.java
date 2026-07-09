package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.usecase.ChatRoomCommandUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatRoomCommandService implements ChatRoomCommandUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    public ChatRoomCommandService(ChatRoomRepository chatRoomRepository,
                                  ChatRoomParticipantRepository chatRoomParticipantRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
    }

    @Override
    public Long createRoom(Long studyId, Long hostId) {
        ChatRoom chatRoom = ChatRoom.create(studyId, hostId);
        ChatRoom saved = chatRoomRepository.save(chatRoom);

        chatRoomParticipantRepository.save(ChatRoomParticipant.create(saved.getId(), hostId));

        return saved.getId();
    }

    @Override
    public void addParticipant(Long chatRoomId, Long memberId) {
        chatRoomParticipantRepository.save(ChatRoomParticipant.create(chatRoomId, memberId));
    }

    @Override
    public void removeParticipant(Long chatRoomId, Long memberId) {
        chatRoomParticipantRepository.deleteByChatRoomIdAndMemberId(chatRoomId, memberId);
    }
}
