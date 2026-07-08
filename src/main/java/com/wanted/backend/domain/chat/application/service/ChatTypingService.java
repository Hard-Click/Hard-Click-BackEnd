package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.event.TypingEvent;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.usecase.ChatTypingUseCase;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ChatTypingService implements ChatTypingUseCase {

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatTypingService(ChatRoomParticipantRepository chatRoomParticipantRepository,
                             MemberNamePort memberNamePort,
                             SimpMessagingTemplate messagingTemplate) {
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyTyping(Long chatRoomId, Long memberId) {
        if (!chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }

        String name = maskName(resolveName(memberId));

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + chatRoomId,
                TypingEvent.of(memberId, name));
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
