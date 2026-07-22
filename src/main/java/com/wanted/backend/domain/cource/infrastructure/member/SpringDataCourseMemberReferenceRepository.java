package com.wanted.backend.domain.cource.infrastructure.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseMemberReferenceRepository
        extends JpaRepository<CourseMemberReferenceEntity, Long> {
}
