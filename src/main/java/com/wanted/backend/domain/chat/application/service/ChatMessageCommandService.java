package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageCommandUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class ChatMessageCommandService implements ChatMessageCommandUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberNamePort memberNamePort;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageCommandService(ChatRoomRepository chatRoomRepository,
                                     ChatRoomParticipantRepository chatRoomParticipantRepository,
                                     ChatMessageRepository chatMessageRepository,
                                     MemberNamePort memberNamePort,
                                     SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.memberNamePort = memberNamePort;
        this.messagingTemplate = messagingTemplate;
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

        String senderName = maskName(resolveName(command.senderId()));

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + command.chatRoomId(),
                ChatMessageEvent.of(saved, senderName));
    }

    private String resolveName(Long memberId) {
        try {
            return memberNamePort.getNamesByMemberIds(Set.of(memberId)).get(memberId);
        } catch (Exception e) {
            return null;
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
