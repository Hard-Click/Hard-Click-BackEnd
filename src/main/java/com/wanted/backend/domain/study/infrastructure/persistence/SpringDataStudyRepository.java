package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.StudyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataStudyRepository extends JpaRepository<StudyJpaEntity, Long> {
    // 모집 목록은 해산(DISSOLVED)된 스터디를 노출하지 않는다(#586) — StatusNot으로 제외.
    List<StudyJpaEntity> findByStatusNot(StudyStatus status, Pageable pageable);

    List<StudyJpaEntity> findBySubjectAndStatusNot(String subject, StudyStatus status, Pageable pageable);

    int countByStatusNot(StudyStatus status);

    int countBySubjectAndStatusNot(String subject, StudyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudyJpaEntity s where s.id = :id")
    Optional<StudyJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
