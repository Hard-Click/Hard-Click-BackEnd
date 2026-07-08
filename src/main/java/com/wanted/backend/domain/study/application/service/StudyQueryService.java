package com.wanted.backend.domain.study.application.service;

import com.wanted.backend.domain.study.application.port.ChatRoomQueryPort;
import com.wanted.backend.domain.study.application.port.MemberNamePort;
import com.wanted.backend.domain.study.application.result.StudyDetailResult;
import com.wanted.backend.domain.study.application.usecase.StudyQueryUseCase;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudyQueryService implements StudyQueryUseCase {

    private final StudyRepository studyRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final ChatRoomQueryPort chatRoomQueryPort;

    public StudyQueryService(StudyRepository studyRepository,
                             StudyParticipantRepository studyParticipantRepository,
                             MemberNamePort memberNamePort,
                             ChatRoomQueryPort chatRoomQueryPort) {
        this.studyRepository = studyRepository;
        this.studyParticipantRepository = studyParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.chatRoomQueryPort = chatRoomQueryPort;
    }

    @Override
    public StudyDetailResult getDetail(Long groupId, Long memberId) {
        Study study = studyRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        List<Long> participantIds = studyParticipantRepository.findMemberIdsByStudyId(groupId);
        boolean isJoined = participantIds.contains(memberId);
        boolean isMine = study.isOwner(memberId);

        Map<Long, String> nameMap = resolveNameMap(participantIds);

        String authorName = maskName(nameMap.getOrDefault(study.getHostId(), ""));
        List<String> members = isJoined
                ? participantIds.stream().map(id -> maskName(nameMap.getOrDefault(id, ""))).toList()
                : null;

        Long chatRoomId = chatRoomQueryPort.findChatRoomIdByStudyId(groupId).orElse(null);

        return new StudyDetailResult(
                study.getId(), study.getTitle(), study.getContent(), study.getSubject(), authorName,
                study.getCurrentCount(), study.getMaxCount(), isMine, isJoined,
                study.getStatus() == StudyStatus.CLOSED, members, chatRoomId, study.getCreatedAt());
    }

    private Map<Long, String> resolveNameMap(List<Long> memberIds) {
        Set<Long> ids = Set.copyOf(memberIds);
        try {
            return memberNamePort.getNamesByMemberIds(ids);
        } catch (Exception e) {
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
