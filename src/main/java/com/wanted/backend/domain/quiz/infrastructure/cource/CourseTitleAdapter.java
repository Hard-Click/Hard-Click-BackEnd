package com.wanted.backend.domain.quiz.infrastructure.cource;

import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseTitleAdapter implements CourseTitlePort {

    private final CourseReferenceJpaRepository courseReferenceJpaRepository;

    @Override
    public Map<Long, String> findTitlesByCourseIds(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        return courseReferenceJpaRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(CourseReferenceJpaEntity::getId,
                        CourseReferenceJpaEntity::getTitle));
    }
}
