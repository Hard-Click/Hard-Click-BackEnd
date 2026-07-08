package com.wanted.backend.domain.study.application.service;

import com.wanted.backend.domain.study.application.port.MemberNamePort;
import com.wanted.backend.domain.study.application.result.StudyItemResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import com.wanted.backend.domain.study.application.usecase.StudyQueryUseCase;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
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
    private final MemberNamePort memberNamePort;

    public StudyQueryService(StudyRepository studyRepository, MemberNamePort memberNamePort) {
        this.studyRepository = studyRepository;
        this.memberNamePort = memberNamePort;
    }

    @Override
    public StudyListResult getList(String subject, int page, int size) {
        List<Study> studies = studyRepository.findAll(subject, page, size);
        int totalCount = studyRepository.countAll(subject);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Set<Long> hostIds = studies.stream().map(Study::getHostId).collect(Collectors.toSet());
        Map<Long, String> nameMap = memberNamePort.getNamesByMemberIds(hostIds);

        List<StudyItemResult> items = studies.stream()
                .map(study -> toItemResult(study, nameMap))
                .toList();

        return new StudyListResult(items, totalPages);
    }

    private StudyItemResult toItemResult(Study study, Map<Long, String> nameMap) {
        String name = maskName(nameMap.getOrDefault(study.getHostId(), ""));
        return new StudyItemResult(
                study.getId(), study.getTitle(), study.getContent(), name, study.getSubject(),
                study.getCurrentCount(), study.getMaxCount(),
                study.getStatus() == StudyStatus.CLOSED, study.getCreatedAt());
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
