package com.wanted.backend.domain.study_timer.infrastructure.persistence;

import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSession;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSessionStatus;
import com.wanted.backend.domain.study_timer.domain.repository.StudyTimerSessionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimerSessionRepositoryAdapter implements StudyTimerSessionRepository {

    private static final List<StudyTimerSessionStatus> ACTIVE_STATUSES = List.of(
            StudyTimerSessionStatus.RUNNING,
            StudyTimerSessionStatus.PAUSED
    );

    private final SpringDataStudyTimerSessionRepository repository;
    private final Clock clock;

    @Override
    public List<StudyTimerSession> findEndedSessionsByDate(Long memberId, LocalDate date) {
        // 날짜 경계는 Clock(KST) 기준 wall-clock. started_at 도 같은 존으로 저장되므로 LocalDateTime 범위로 비교.
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime toExclusive = date.plusDays(1).atStartOfDay();
        return repository.findByMemberIdAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        memberId, StudyTimerSessionStatus.ENDED, from, toExclusive)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsRunningByMemberId(Long memberId) {
        return repository.existsByMemberIdAndStatus(memberId, StudyTimerSessionStatus.RUNNING);
    }

    @Override
    public boolean existsActiveByMemberId(Long memberId) {
        return repository.existsByMemberIdAndStatusIn(memberId, ACTIVE_STATUSES);
    }

    @Override
    public Optional<StudyTimerSession> findRunningByMemberId(Long memberId) {
        return repository.findByMemberIdAndStatus(memberId, StudyTimerSessionStatus.RUNNING)
                .map(this::toDomain);
    }

    @Override
    public Optional<StudyTimerSession> findActiveByMemberId(Long memberId) {
        return repository.findFirstByMemberIdAndStatusInOrderByStartedAtDescIdDesc(memberId, ACTIVE_STATUSES)
                .map(this::toDomain);
    }

    @Override
    public Optional<StudyTimerSession> findById(Long sessionId) {
        return repository.findById(sessionId).map(this::toDomain);
    }

    @Override
    @Transactional
    public StudyTimerSession save(StudyTimerSession session) {
        LocalDateTime now = LocalDateTime.now(clock);
        StudyTimerSessionJpaEntity entity = session.id() == null
                ? new StudyTimerSessionJpaEntity(
                session.memberId(),
                session.courseId(),
                session.lessonId(),
                toLocalDateTime(session.startedAt()),
                toLocalDateTime(session.endedAt()),
                session.accumulatedStudySeconds(),
                session.status(),
                now,
                now
        )
                : repository.findById(session.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_TIMER_SESSION_NOT_FOUND));

        entity.updateSession(
                session.courseId(),
                session.lessonId(),
                toLocalDateTime(session.startedAt()),
                toLocalDateTime(session.endedAt()),
                session.accumulatedStudySeconds(),
                session.status(),
                toLocalDateTime(session.pausedAt()),
                now
        );

        return toDomain(repository.saveAndFlush(entity));
    }

    private StudyTimerSession toDomain(StudyTimerSessionJpaEntity entity) {
        return new StudyTimerSession(
                entity.getId(),
                entity.getMemberId(),
                entity.getCourseId(),
                entity.getLessonId(),
                toOffsetDateTime(entity.getStartedAt()),
                toOffsetDateTime(entity.getEndedAt()),
                entity.getAccumulatedStudySeconds(),
                entity.getStatus(),
                toOffsetDateTime(entity.getPausedAt())
        );
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZoneSameInstant(clock.getZone()).toLocalDateTime();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(clock.getZone()).toOffsetDateTime();
    }
}
