package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.result.ChatMessageDetail;
import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageQueryUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatMessageQueryService implements ChatMessageQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageQueryService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberNamePort memberNamePort;

    public ChatMessageQueryService(ChatRoomRepository chatRoomRepository,
                                   ChatRoomParticipantRepository chatRoomParticipantRepository,
                                   ChatMessageRepository chatMessageRepository,
                                   MemberNamePort memberNamePort) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.memberNamePort = memberNamePort;
    }

    @Override
    public ChatMessageListResult getMessages(Long chatRoomId, Long cursorId, int size, Long memberId) {
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }

        List<ChatMessage> fetched = chatMessageRepository.findByChatRoomIdBeforeCursor(chatRoomId, cursorId, size + 1);

        boolean hasNext = fetched.size() > size;
        List<ChatMessage> page = hasNext ? fetched.subList(0, size) : fetched;

        List<ChatMessage> ascending = new ArrayList<>(page);
        Collections.reverse(ascending);

        Set<Long> senderIds = ascending.stream()
                .map(ChatMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = resolveNameMap(senderIds);

        List<ChatMessageDetail> messages = ascending.stream()
                .map(message -> toDetail(message, nameMap))
                .toList();

        Long nextCursorId = ascending.isEmpty() ? null : ascending.get(0).getId();

        return new ChatMessageListResult(messages, hasNext, nextCursorId);
    }

    private ChatMessageDetail toDetail(ChatMessage message, Map<Long, String> nameMap) {
        Long senderId = message.getSenderId();
        String senderName = senderId == null ? null : maskName(nameMap.get(senderId));
        return new ChatMessageDetail(
                message.getType().name(), message.getId(), senderId, senderName,
                message.getContent(), message.getSentAt());
    }

    private Map<Long, String> resolveNameMap(Collection<Long> memberIds) {
        try {
            return memberNamePort.getNamesByMemberIds(memberIds);
        } catch (Exception e) {
            log.warn("MemberNamePort 호출 실패, 빈 이름 맵으로 fallback. memberIds={}", memberIds, e);
            return Map.of();
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
