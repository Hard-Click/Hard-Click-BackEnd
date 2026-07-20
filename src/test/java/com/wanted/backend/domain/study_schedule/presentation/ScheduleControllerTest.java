package com.wanted.backend.domain.study_schedule.presentation;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import com.wanted.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * '오늘'을 서버 JVM 기본 타임존이 아니라 주입된 Clock(KST)으로 계산하는지 못 박는 회귀 테스트.
 *
 * <p>버그: 컨트롤러가 인자 없는 {@code LocalDate.now()} 를 써서 컨테이너 기본 타임존(UTC)에 의존했다.
 * KST 00:00~09:00 구간엔 UTC 기준 '오늘'이 전날이라 /me/today 가 빈 배열을 반환했다.
 * 여기선 UTC 22:00(= KST 익일 07:00) 순간에 Clock 을 고정해, 다운스트림으로 넘어가는 날짜가
 * UTC 날짜가 아니라 KST 날짜인지 검증한다. Clock 주입을 제거하면 이 테스트가 깨진다.
 */
class ScheduleControllerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 2026-07-19 22:00 UTC == 2026-07-20 07:00 KST → UTC 날짜(19일)와 KST 날짜(20일)가 갈리는 순간.
    private static final Instant BOUNDARY = Instant.parse("2026-07-19T22:00:00Z");
    private static final Long MEMBER_ID = 77001L;

    private ScheduleUseCase scheduleUseCase;
    private CustomUserDetails userDetails;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        scheduleUseCase = mock(ScheduleUseCase.class);
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getMemberId()).thenReturn(MEMBER_ID);
        controller = new ScheduleController(scheduleUseCase, Clock.fixed(BOUNDARY, KST));
    }

    @Test
    void today_usesKstDateNotServerUtcDate() {
        when(scheduleUseCase.getMyToday(anyLong(), any()))
                .thenReturn(new ScheduleDtos.TodayView(List.of(), 0, 0));

        controller.getMyToday(userDetails);

        ArgumentCaptor<LocalDate> today = ArgumentCaptor.forClass(LocalDate.class);
        verify(scheduleUseCase).getMyToday(eq(MEMBER_ID), today.capture());
        // UTC 로 계산하면 07-19 가 넘어가 빈 배열이 됐다. KST 기준이면 07-20.
        assertThat(today.getValue()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void schedule_defaultsToKstCurrentMonthWhenRangeOmitted() {
        when(scheduleUseCase.getMySchedule(anyLong(), any(), any())).thenReturn(List.of());

        controller.getMySchedule(userDetails, null, null);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(scheduleUseCase).getMySchedule(eq(MEMBER_ID), from.capture(), to.capture());
        // KST 기준 7월. UTC 로 계산하면 여전히 7월이지만 경계일엔 '이번 달' 판정이 어긋날 수 있어 KST 로 고정한다.
        assertThat(from.getValue()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(to.getValue()).isEqualTo(LocalDate.of(2026, 7, 31));
    }
}
