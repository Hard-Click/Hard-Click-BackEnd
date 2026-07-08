package com.wanted.backend.domain.study.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudyParticipantRepository extends JpaRepository<StudyParticipantJpaEntity, Long> {
}
