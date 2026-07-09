package com.wanted.backend.domain.chat.application.service;

import com.wanted.backend.domain.chat.application.port.ChatPresencePort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.application.port.StudyInfoQueryPort;
import com.wanted.backend.domain.chat.application.port.StudyInfoResult;
import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.ParticipantDetail;
import com.wanted.backend.domain.chat.application.usecase.ChatRoomQueryUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ChatRoomQueryService implements ChatRoomQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomQueryService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final ChatPresencePort chatPresencePort;
    private final StudyInfoQueryPort studyInfoQueryPort;

    public ChatRoomQueryService(ChatRoomRepository chatRoomRepository,
                                ChatRoomParticipantRepository chatRoomParticipantRepository,
                                MemberNamePort memberNamePort,
                                ChatPresencePort chatPresencePort,
                                StudyInfoQueryPort studyInfoQueryPort) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.chatPresencePort = chatPresencePort;
        this.studyInfoQueryPort = studyInfoQueryPort;
    }

    @Override
    public ChatRoomDetailResult getRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }

        StudyInfoResult studyInfo = studyInfoQueryPort.getStudyInfo(chatRoom.getStudyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        List<Long> participantIds = chatRoomParticipantRepository.findMemberIdsByChatRoomId(chatRoomId);
        Map<Long, String> nameMap = resolveNameMap(participantIds);
        Set<Long> onlineMemberIds = resolveOnlineMemberIds(chatRoomId);

        List<ParticipantDetail> participants = participantIds.stream()
                .map(id -> new ParticipantDetail(id, maskName(nameMap.get(id)), onlineMemberIds.contains(id)))
                .toList();

        return new ChatRoomDetailResult(
                chatRoom.getId(), chatRoom.getStudyId(), studyInfo.title(), studyInfo.subject(),
                chatRoom.getHostId(), chatRoom.getStatus().name(), participants, participants.size());
    }

    private Map<Long, String> resolveNameMap(Collection<Long> memberIds) {
        try {
            return memberNamePort.getNamesByMemberIds(memberIds);
        } catch (Exception e) {
            log.warn("MemberNamePort 호출 실패, 빈 이름 맵으로 fallback. memberIds={}", memberIds, e);
            return Map.of();
        }
    }

    private Set<Long> resolveOnlineMemberIds(Long chatRoomId) {
        try {
            return chatPresencePort.getOnlineMemberIds(chatRoomId);
        } catch (Exception e) {
            log.warn("ChatPresencePort 호출 실패, 빈 Set으로 fallback. chatRoomId={}", chatRoomId, e);
            return Set.of();
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
