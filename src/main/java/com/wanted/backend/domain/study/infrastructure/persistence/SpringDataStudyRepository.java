package com.wanted.backend.domain.study.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataStudyRepository extends JpaRepository<StudyJpaEntity, Long> {
    List<StudyJpaEntity> findBySubject(String subject, Pageable pageable);

    int countBySubject(String subject);
}
