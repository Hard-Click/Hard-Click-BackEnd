package com.wanted.backend.domain.study_timer.application.service;

import com.wanted.backend.domain.study_timer.application.query.GetStudyTimerSessionsByDateQuery;
import com.wanted.backend.domain.study_timer.application.usecase.GetStudyTimerSessionsByDateUseCase.StudyTimerSessionView;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSession;
import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSessionStatus;
import com.wanted.backend.domain.study_timer.domain.repository.StudyTimerSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudyTimerSessionsByDateServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 21);
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    @Mock
    private StudyTimerSessionRepository studyTimerSessionRepository;

    private GetStudyTimerSessionsByDateService service;

    @BeforeEach
    void setUp() {
        service = new GetStudyTimerSessionsByDateService(studyTimerSessionRepository);
    }

    private static StudyTimerSession endedSession(Long id, OffsetDateTime startedAt, OffsetDateTime endedAt) {
        return new StudyTimerSession(
                id, MEMBER_ID, null, null, startedAt, endedAt, 3600,
                StudyTimerSessionStatus.ENDED, null);
    }

    @Test
    void mapsEndedSessionsToViewsPreservingStartAndEnd() {
        OffsetDateTime start = OffsetDateTime.of(2026, 7, 21, 9, 0, 0, 0, KST);
        OffsetDateTime end = OffsetDateTime.of(2026, 7, 21, 10, 30, 0, 0, KST);
        when(studyTimerSessionRepository.findEndedSessionsByDate(MEMBER_ID, DATE))
                .thenReturn(List.of(endedSession(55L, start, end)));

        List<StudyTimerSessionView> views = service.handle(new GetStudyTimerSessionsByDateQuery(MEMBER_ID, DATE));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).sessionId()).isEqualTo(55L);
        assertThat(views.get(0).startedAt()).isEqualTo(start);
        assertThat(views.get(0).endedAt()).isEqualTo(end);
    }

    @Test
    void returnsEmptyListWhenNoSessions() {
        when(studyTimerSessionRepository.findEndedSessionsByDate(anyLong(), any()))
                .thenReturn(List.of());

        assertThat(service.handle(new GetStudyTimerSessionsByDateQuery(MEMBER_ID, DATE))).isEmpty();
    }
}
