package com.wanted.backend.domain.study.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataStudyParticipantRepository extends JpaRepository<StudyParticipantJpaEntity, Long> {
    List<StudyParticipantJpaEntity> findByStudyId(Long studyId);
}
