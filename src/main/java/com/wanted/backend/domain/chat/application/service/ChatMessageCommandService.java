package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.event.ChatMessagePersistedEvent;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageCommandUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatMessageCommandService implements ChatMessageCommandUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChatMessageCommandService(ChatRoomRepository chatRoomRepository,
                                     ChatRoomParticipantRepository chatRoomParticipantRepository,
                                     ChatMessageRepository chatMessageRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void send(SendMessageCommand command) {
        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(command.chatRoomId(), command.senderId())) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }
        chatRoom.validateActive();

        ChatMessage message = ChatMessage.create(command.chatRoomId(), command.senderId(), command.content());
        ChatMessage saved = chatMessageRepository.save(message);

        eventPublisher.publishEvent(ChatMessagePersistedEvent.from(saved));
    }
}
