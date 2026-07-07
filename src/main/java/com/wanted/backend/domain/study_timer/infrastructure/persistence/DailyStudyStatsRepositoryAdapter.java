package com.wanted.backend.domain.study_timer.infrastructure.persistence;

import com.wanted.backend.domain.study_timer.domain.model.DailyStudyStat;
import com.wanted.backend.domain.study_timer.domain.repository.DailyStudyStatsRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyStudyStatsRepositoryAdapter implements DailyStudyStatsRepository {

    private final SpringDataDailyStudyStatsRepository repository;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    @Transactional
    public DailyStudyStat upsertStudySeconds(
            Long memberId,
            LocalDate studyDate,
            Integer additionalStudySeconds
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        // 오버플로우 사전 검증 — 호출 전 memberLockPort.lock()이 FOR UPDATE로 직렬화하므로 동시성 안전
        Optional<DailyStudyStatsJpaEntity> existingEntity = repository.findByMemberIdAndStatDate(memberId, studyDate);
        DailyStudyStat current = existingEntity
                .map(this::toDomain)
                .orElseGet(() -> new DailyStudyStat(memberId, studyDate, 0));
        current.increaseStudySeconds(additionalStudySeconds);   // 오버플로우 시 예외

        entityManager.createNativeQuery("""
                INSERT INTO daily_study_stats
                    (member_id, stat_date, watched_lesson_count, study_seconds, completed_lesson_count, created_at, updated_at)
                VALUES
                    (:memberId, :statDate, 0, :delta, 0, :now, :now)
                ON DUPLICATE KEY UPDATE
                    study_seconds = study_seconds + :delta,
                    updated_at = :now
                """)
                .setParameter("memberId", memberId)
                .setParameter("statDate", studyDate)
                .setParameter("delta", additionalStudySeconds)
                .setParameter("now", now)
                .executeUpdate();
        // clear() 대신 해당 엔티티만 detach — 같은 트랜잭션 내 다른 관리 엔티티를 건드리지 않는다
        existingEntity.ifPresent(entityManager::detach);
        return repository.findByMemberIdAndStatDate(memberId, studyDate)
                .map(this::toDomain)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_TIMER_DAILY_STAT_INVALID));
    }

    @Override
    public List<DailyStudyStat> findByMemberIdAndDateBetween(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return repository.findByMemberIdAndStatDateBetweenOrderByStatDateAsc(memberId, startDate, endDate).stream()
                .map(this::toDomain)
                .toList();
    }

    private DailyStudyStat toDomain(DailyStudyStatsJpaEntity entity) {
        return new DailyStudyStat(
                entity.getMemberId(),
                entity.getStatDate(),
                entity.getStudySeconds()
        );
    }
}
