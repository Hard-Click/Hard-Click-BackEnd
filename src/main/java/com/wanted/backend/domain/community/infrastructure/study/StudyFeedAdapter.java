package com.wanted.backend.domain.community.infrastructure.study;

import com.wanted.backend.domain.community.application.port.MemberNamePort;
import com.wanted.backend.domain.community.application.port.StudyFeedPort;
import com.wanted.backend.domain.community.domain.model.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyFeedAdapter implements StudyFeedPort {

    // status != ACTIVE 면 모집 마감(FULL). DISSOLVED 는 애초에 조회에서 제외된다.
    private static final String STATUS_DISSOLVED = "DISSOLVED";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final StudyReferenceRepository studyReferenceRepository;
    private final MemberNamePort memberNamePort;

    @Override
    public List<StudyFeedItem> findActiveStudies() {
        List<StudyReferenceEntity> studies = studyReferenceRepository.findByStatusNot(STATUS_DISSOLVED);
        if (studies.isEmpty()) {
            return List.of();
        }

        Set<Long> hostIds = studies.stream().map(StudyReferenceEntity::getHostId).collect(Collectors.toSet());
        Map<Long, String> nameMap = memberNamePort.getNamesByMemberIds(hostIds);

        return studies.stream()
                .map(study -> new StudyFeedItem(
                        study.getId(),
                        study.getTitle(),
                        // 스터디 탭과 동일하게 작성자명은 마스킹해서 노출한다.
                        Review.maskName(nameMap.getOrDefault(study.getHostId(), "")),
                        study.getSubject(),
                        study.getCurrentCount(),
                        study.getMaxCount(),
                        !STATUS_ACTIVE.equals(study.getStatus()),
                        study.getCreatedAt()))
                .toList();
    }
}
