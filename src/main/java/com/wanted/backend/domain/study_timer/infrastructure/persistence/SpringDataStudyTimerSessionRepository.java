package com.wanted.backend.domain.study_timer.infrastructure.persistence;

import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataStudyTimerSessionRepository extends JpaRepository<StudyTimerSessionJpaEntity, Long> {

    // started_at ∈ [from, toExclusive) 인 특정 상태 세션을 시작 시각 오름차순으로. 자정 경계는 시작한 날에 귀속.
    List<StudyTimerSessionJpaEntity> findByMemberIdAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
            Long memberId, StudyTimerSessionStatus status, LocalDateTime from, LocalDateTime toExclusive);

    boolean existsByMemberIdAndStatus(Long memberId, StudyTimerSessionStatus status);

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<StudyTimerSessionStatus> statuses);

    Optional<StudyTimerSessionJpaEntity> findByMemberIdAndStatus(Long memberId, StudyTimerSessionStatus status);

    Optional<StudyTimerSessionJpaEntity> findFirstByMemberIdAndStatusInOrderByStartedAtDescIdDesc(
            Long memberId,
            Collection<StudyTimerSessionStatus> statuses
    );
}
