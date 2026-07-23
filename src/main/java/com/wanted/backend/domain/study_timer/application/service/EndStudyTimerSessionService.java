package com.wanted.backend.domain.study_timer.application.service;

import com.wanted.backend.domain.study_timer.application.command.EndStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.port.MemberLockPort;
import com.wanted.backend.domain.study_timer.application.usecase.EndStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.domain.event.StudySessionEndedEvent;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSession;
import com.wanted.backend.domain.study_timer.domain.repository.DailyStudyStatsRepository;
import com.wanted.backend.domain.study_timer.domain.repository.StudyTimerSessionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EndStudyTimerSessionService implements EndStudyTimerSessionUseCase {

    private static final StudyTimerAction ACTION = StudyTimerAction.END;

    private final MemberLockPort memberLockPort;
    private final StudyTimerSessionRepository studyTimerSessionRepository;
    private final DailyStudyStatsRepository dailyStudyStatsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StudyTimerSessionMetricRecorder metricRecorder;
    private final Clock clock;

    @Override
    @Transactional
    public StudyTimerSessionEndView handle(EndStudyTimerSessionCommand command) {
        String errorCode = "UNKNOWN";
        try {
            OffsetDateTime serverNow = OffsetDateTime.now(clock);
            validateEndedAt(command.endedAt(), serverNow);

            memberLockPort.lock(command.memberId());

            StudyTimerSession session = studyTimerSessionRepository.findById(command.sessionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_TIMER_SESSION_NOT_FOUND));

            if (!session.isOwnedBy(command.memberId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            StudyTimerSession endedSession = session.end(command.endedAt(), serverNow);
            // heartbeat/pause는 daily·랭킹 집계에 반영하지 않으므로, 종료 시 세션 '전체 누적'을 집계에 반영한다.
            // 마지막 heartbeat 이후 증가분(꼬리)만 반영하면 세션 시간 대부분이 집계에서 누락된다.
            // end()는 세션당 1회만 성공(RUNNING 아니면 예외)하므로 전체 누적을 반영해도 이중 계산되지 않는다.
            int sessionStudySeconds = endedSession.accumulatedStudySeconds();
            LocalDate studyDate = command.endedAt().atZoneSameInstant(clock.getZone()).toLocalDate();

            StudyTimerSession saved = studyTimerSessionRepository.save(endedSession);
            if (sessionStudySeconds > 0) {
                dailyStudyStatsRepository.upsertStudySeconds(command.memberId(), studyDate, sessionStudySeconds);
                eventPublisher.publishEvent(StudySessionEndedEvent.of(
                        command.memberId(),
                        studyDate,
                        sessionStudySeconds,
                        command.endedAt()
                ));
            }

            errorCode = null;
            return new StudyTimerSessionEndView(
                    saved.id(),
                    saved.accumulatedStudySeconds(),
                    saved.status().name(),
                    command.endedAt()
            );
        } catch (BusinessException e) {
            errorCode = e.getErrorCode().name();
            throw e;
        } finally {
            try {
                metricRecorder.recordResult(ACTION, errorCode);
            } catch (RuntimeException e) {
                // metric failure must not affect the business transaction
                log.warn("study timer metric record failed: action={}, errorCode={}", ACTION, errorCode, e);
            }
        }
    }

    private void validateEndedAt(OffsetDateTime endedAt, OffsetDateTime serverNow) {
        if (endedAt == null) {
            throw new BusinessException(ErrorCode.STUDY_TIMER_ENDED_AT_REQUIRED);
        }
        if (endedAt.toInstant().isAfter(serverNow.toInstant())) {
            throw new BusinessException(ErrorCode.STUDY_TIMER_ENDED_AT_IN_FUTURE);
        }
    }
}
