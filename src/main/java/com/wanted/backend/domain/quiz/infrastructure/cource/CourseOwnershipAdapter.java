package com.wanted.backend.domain.quiz.infrastructure.cource;

import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.domain.quiz.application.port.CourseOwnershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourseOwnershipAdapter implements CourseOwnershipPort {

    private final CourseRepository courseRepository;
    private final CourseSectionReferenceJpaRepository courseSectionReferenceJpaRepository;

    @Override
    public Optional<CourseSectionOwnership> findOwnership(Long courseId, Long sectionId) {
        return courseRepository.findById(courseId)
                .filter(course -> !course.isDeleted())
                .map(course -> new CourseSectionOwnership(
                        course.getAuthorId(),
                        courseSectionReferenceJpaRepository.existsByIdAndCourseId(sectionId, courseId)
                ));
    }
}
