package com.wanted.backend.domain.quiz.infrastructure.cource;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseSectionTitleAdapter implements CourseSectionTitlePort {

    private final CourseSectionReferenceJpaRepository courseSectionReferenceJpaRepository;

    @Override
    public Map<Long, String> findTitlesBySectionIds(Collection<Long> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }

        return courseSectionReferenceJpaRepository.findAllById(sectionIds).stream()
                .collect(Collectors.toMap(CourseSectionReferenceJpaEntity::getId,
                        CourseSectionReferenceJpaEntity::getTitle));
    }

    @Override
    public Map<Long, SectionInfo> findSectionsByIds(Collection<Long> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }

        return courseSectionReferenceJpaRepository.findAllById(sectionIds).stream()
                .collect(Collectors.toMap(CourseSectionReferenceJpaEntity::getId,
                        e -> new SectionInfo(e.getTitle(), e.getOrderIndex())));
    }
}
