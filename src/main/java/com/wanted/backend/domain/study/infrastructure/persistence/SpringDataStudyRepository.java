package com.wanted.backend.domain.study.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataStudyRepository extends JpaRepository<StudyJpaEntity, Long> {
    List<StudyJpaEntity> findBySubject(String subject, Pageable pageable);

    int countBySubject(String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudyJpaEntity s where s.id = :id")
    Optional<StudyJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
