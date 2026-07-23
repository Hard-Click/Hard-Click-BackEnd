package com.wanted.backend.domain.study_timer.application.service;

import com.wanted.backend.domain.study_timer.application.command.EndStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.port.MemberLockPort;
import com.wanted.backend.domain.study_timer.application.usecase.EndStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.domain.event.StudySessionEndedEvent;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSession;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSessionStatus;
import com.wanted.backend.domain.study_timer.domain.repository.DailyStudyStatsRepository;
import com.wanted.backend.domain.study_timer.domain.repository.StudyTimerSessionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndStudyTimerSessionServiceTest {

    private MemberLockPort memberLockPort;
    private StudyTimerSessionRepository repository;
    private DailyStudyStatsRepository dailyStudyStatsRepository;
    private ApplicationEventPublisher eventPublisher;
    private StudyTimerSessionMetricRecorder metricRecorder;
    private EndStudyTimerSessionService service;

    @BeforeEach
    void setUp() {
        memberLockPort = mock(MemberLockPort.class);
        repository = mock(StudyTimerSessionRepository.class);
        dailyStudyStatsRepository = mock(DailyStudyStatsRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        metricRecorder = mock(StudyTimerSessionMetricRecorder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-11T06:10:00Z"), ZoneId.of("Asia/Seoul"));
        service = new EndStudyTimerSessionService(
                memberLockPort,
                repository,
                dailyStudyStatsRepository,
                eventPublisher,
                metricRecorder,
                clock
        );
    }

    @Test
    void endsRunningSessionAfterLockingAndLoadingOwnedSession() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-11T15:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-05-11T15:08:20+09:00");
        EndStudyTimerSessionCommand command = new EndStudyTimerSessionCommand(1L, 55L, endedAt);
        StudyTimerSession runningSession = new StudyTimerSession(
                55L,
                1L,
                null,
                null,
                startedAt,
                null,
                200,
                StudyTimerSessionStatus.RUNNING
        );

        when(repository.findById(55L)).thenReturn(Optional.of(runningSession));
        when(repository.save(any(StudyTimerSession.class)))
                .thenReturn(new StudyTimerSession(
                        55L,
                        1L,
                        null,
                        null,
                        startedAt,
                        endedAt,
                        500,
                        StudyTimerSessionStatus.ENDED
                ));

        EndStudyTimerSessionUseCase.StudyTimerSessionEndView result = service.handle(command);

        ArgumentCaptor<StudyTimerSession> captor = ArgumentCaptor.forClass(StudyTimerSession.class);
        ArgumentCaptor<StudySessionEndedEvent> eventCaptor = ArgumentCaptor.forClass(StudySessionEndedEvent.class);
        InOrder inOrder = inOrder(memberLockPort, repository);
        inOrder.verify(memberLockPort).lock(1L);
        inOrder.verify(repository).findById(55L);
        inOrder.verify(repository).save(captor.capture());

        assertThat(captor.getValue().id()).isEqualTo(55L);
        assertThat(captor.getValue().status()).isEqualTo(StudyTimerSessionStatus.ENDED);
        assertThat(captor.getValue().endedAt()).isEqualTo(endedAt);
        assertThat(captor.getValue().accumulatedStudySeconds()).isEqualTo(500);
        assertThat(result.sessionId()).isEqualTo(55L);
        assertThat(result.accumulatedStudySeconds()).isEqualTo(500);
        assertThat(result.status()).isEqualTo("ENDED");
        assertThat(result.endedAt()).isEqualTo(endedAt);
        verify(dailyStudyStatsRepository).upsertStudySeconds(1L, LocalDate.parse("2026-05-11"), 500);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memberId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().studyDate()).isEqualTo(LocalDate.parse("2026-05-11"));
        assertThat(eventCaptor.getValue().deltaStudySeconds()).isEqualTo(500);
        assertThat(eventCaptor.getValue().endedAt()).isEqualTo(endedAt);
        verify(metricRecorder).recordResult(StudyTimerAction.END, null);
    }

    @Test
    void businessResultIsUnaffectedWhenMetricRecordingFails() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-11T15:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-05-11T15:08:20+09:00");
        when(repository.findById(55L)).thenReturn(Optional.of(new StudyTimerSession(
                55L, 1L, null, null, startedAt, null, 200, StudyTimerSessionStatus.RUNNING
        )));
        when(repository.save(any(StudyTimerSession.class)))
                .thenReturn(new StudyTimerSession(
                        55L, 1L, null, null, startedAt, endedAt, 500, StudyTimerSessionStatus.ENDED
                ));
        doThrow(new RuntimeException("metric registry down"))
                .when(metricRecorder).recordResult(StudyTimerAction.END, null);

        EndStudyTimerSessionUseCase.StudyTimerSessionEndView result =
                service.handle(new EndStudyTimerSessionCommand(1L, 55L, endedAt));

        assertThat(result.sessionId()).isEqualTo(55L);
        assertThat(result.accumulatedStudySeconds()).isEqualTo(500);
        assertThat(result.status()).isEqualTo("ENDED");
        assertThat(result.endedAt()).isEqualTo(endedAt);
        verify(dailyStudyStatsRepository).upsertStudySeconds(1L, LocalDate.parse("2026-05-11"), 500);
        verify(eventPublisher).publishEvent(any(Object.class));
        verify(metricRecorder).recordResult(StudyTimerAction.END, null);
    }

    @Test
    void skipsDailyStatsUpsertAndEventWhenSessionAccumulatedIsZero() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-11T15:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-05-11T15:00:00+09:00");
        StudyTimerSession runningSession = new StudyTimerSession(
                55L,
                1L,
                null,
                null,
                startedAt,
                null,
                0,
                StudyTimerSessionStatus.RUNNING
        );

        when(repository.findById(55L)).thenReturn(Optional.of(runningSession));
        when(repository.save(any(StudyTimerSession.class)))
                .thenReturn(new StudyTimerSession(
                        55L,
                        1L,
                        null,
                        null,
                        startedAt,
                        endedAt,
                        0,
                        StudyTimerSessionStatus.ENDED
                ));

        service.handle(new EndStudyTimerSessionCommand(1L, 55L, endedAt));

        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void creditsFullSessionAccumulatedNotTailSinceLastHeartbeat() {
        // 회귀 방지(실측 버그 session 68): 마지막 heartbeat 누적(1092)과 종료 누적(1093)의
        // 꼬리(1초)가 아니라, 세션 전체 누적(1093)이 daily·랭킹에 반영되어야 한다.
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-11T14:51:47+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-05-11T15:10:00+09:00");
        StudyTimerSession runningSession = new StudyTimerSession(
                55L,
                1L,
                null,
                null,
                startedAt,
                null,
                1092,
                StudyTimerSessionStatus.RUNNING
        );

        when(repository.findById(55L)).thenReturn(Optional.of(runningSession));
        when(repository.save(any(StudyTimerSession.class)))
                .thenReturn(new StudyTimerSession(
                        55L,
                        1L,
                        null,
                        null,
                        startedAt,
                        endedAt,
                        1093,
                        StudyTimerSessionStatus.ENDED
                ));

        ArgumentCaptor<StudySessionEndedEvent> eventCaptor = ArgumentCaptor.forClass(StudySessionEndedEvent.class);

        service.handle(new EndStudyTimerSessionCommand(1L, 55L, endedAt));

        verify(dailyStudyStatsRepository).upsertStudySeconds(1L, LocalDate.parse("2026-05-11"), 1093);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().deltaStudySeconds()).isEqualTo(1093);
    }

    @Test
    void doesNotPublishEventWhenDailyStatsUpsertFails() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-11T15:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-05-11T15:08:20+09:00");
        EndStudyTimerSessionCommand command = new EndStudyTimerSessionCommand(1L, 55L, endedAt);
        StudyTimerSession runningSession = new StudyTimerSession(
                55L,
                1L,
                null,
                null,
                startedAt,
                null,
                200,
                StudyTimerSessionStatus.RUNNING
        );

        when(repository.findById(55L)).thenReturn(Optional.of(runningSession));
        when(repository.save(any(StudyTimerSession.class)))
                .thenReturn(new StudyTimerSession(
                        55L,
                        1L,
                        null,
                        null,
                        startedAt,
                        endedAt,
                        500,
                        StudyTimerSessionStatus.ENDED
                ));
        when(dailyStudyStatsRepository.upsertStudySeconds(1L, LocalDate.parse("2026-05-11"), 500))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(metricRecorder).recordResult(StudyTimerAction.END, "UNKNOWN");
    }

    @Test
    void throwsNotFoundWhenSessionDoesNotExist() {
        when(repository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new EndStudyTimerSessionCommand(
                1L,
                55L,
                OffsetDateTime.parse("2026-05-11T15:08:20+09:00")
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_TIMER_SESSION_NOT_FOUND);

        verify(memberLockPort).lock(1L);
        verify(repository).findById(55L);
        verify(repository, never()).save(any());
        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void throwsForbiddenWhenSessionOwnerDoesNotMatchLoggedInMember() {
        when(repository.findById(55L)).thenReturn(Optional.of(new StudyTimerSession(
                55L,
                2L,
                null,
                null,
                OffsetDateTime.parse("2026-05-11T15:00:00+09:00"),
                null,
                200,
                StudyTimerSessionStatus.RUNNING
        )));

        assertThatThrownBy(() -> service.handle(new EndStudyTimerSessionCommand(
                1L,
                55L,
                OffsetDateTime.parse("2026-05-11T15:08:20+09:00")
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(memberLockPort).lock(1L);
        verify(repository).findById(55L);
        verify(repository, never()).save(any());
        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void throwsConflictWhenSessionIsNotRunning() {
        when(repository.findById(55L)).thenReturn(Optional.of(new StudyTimerSession(
                55L,
                1L,
                null,
                null,
                OffsetDateTime.parse("2026-05-11T15:00:00+09:00"),
                OffsetDateTime.parse("2026-05-11T15:08:20+09:00"),
                500,
                StudyTimerSessionStatus.ENDED
        )));

        assertThatThrownBy(() -> service.handle(new EndStudyTimerSessionCommand(
                1L,
                55L,
                OffsetDateTime.parse("2026-05-11T15:08:20+09:00")
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_TIMER_SESSION_NOT_RUNNING);

        verify(memberLockPort).lock(1L);
        verify(repository).findById(55L);
        verify(repository, never()).save(any());
        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(metricRecorder).recordResult(StudyTimerAction.END, "STUDY_TIMER_SESSION_NOT_RUNNING");
    }

    @Test
    void throwsInvalidInputWhenEndedAtIsInFuture() {
        assertThatThrownBy(() -> service.handle(new EndStudyTimerSessionCommand(
                1L,
                55L,
                OffsetDateTime.parse("2026-05-11T15:10:01+09:00")
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_TIMER_ENDED_AT_IN_FUTURE);

        verify(memberLockPort, never()).lock(any());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void throwsInvalidInputWhenEndedAtIsNull() {
        assertThatThrownBy(() -> service.handle(new EndStudyTimerSessionCommand(
                1L,
                55L,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_TIMER_ENDED_AT_REQUIRED);

        verify(memberLockPort, never()).lock(any());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(dailyStudyStatsRepository, never()).upsertStudySeconds(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
