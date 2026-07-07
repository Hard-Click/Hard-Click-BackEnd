package com.wanted.backend.domain.quiz.application.port;

import java.util.Optional;

public interface CourseOwnershipPort {

    Optional<CourseSectionOwnership> findOwnership(Long courseId, Long sectionId);

    record CourseSectionOwnership(Long instructorId, boolean sectionBelongsToCourse) {}
}
