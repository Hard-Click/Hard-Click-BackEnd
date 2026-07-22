package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 매일 자정 직후, 계획일이 지났는데 완료 안 된(PLANNED) 학습 슬롯을 MISSED 로 전이하는 배치.
 *
 * <p>"하루 지날 때마다 못 한 학습을 못 함으로 표시"의 원천 - FE 캘린더는 이 MISSED 상태를 받아 검정 막대로 그린다.
 * 학생 할 일(student_todo)은 MISSED 개념이 없어(도메인 규칙: 미달성 판정은 AI 계획에만) 대상이 아니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleMissedScheduler {

    private final ScheduleUseCase scheduleUseCase;
    private final Clock clock;

    @Scheduled(cron = "${schedule.mark-missed.cron:0 5 0 * * *}")
    public void markPastPlannedAsMissed() {
        LocalDate today = LocalDate.now(clock);
        try {
            int updated = scheduleUseCase.markPastPlannedAsMissed(today);
            log.info("[Schedule] past PLANNED slots marked MISSED. today={}, updated={}", today, updated);
        } catch (Exception exception) {
            log.error("[Schedule] mark-missed batch failed. today={}", today, exception);
        }
    }
}
