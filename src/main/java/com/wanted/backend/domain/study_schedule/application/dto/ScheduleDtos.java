package com.wanted.backend.domain.study_schedule.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 학습 스케줄 조회 결과 DTO(application). AI가 선계산해 weekly_schedule/schedule_slot 에 저장한 활성 계획을
 * 학생 캘린더/오늘 할 일 형태로 읽어 온다. (학생은 AI 계획을 편집하지 않고 완료 체크만 — 소유 모델 유지)
 */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    /** 캘린더 한 칸(슬롯). subject = 과목(색상 구분용). */
    public record CalendarItem(
            Long slotId,
            LocalDate planDate,
            LocalTime startTime,
            Long enrollmentId,
            Long courseId,
            String subject,
            String courseTitle,
            Long lessonId,
            String lessonTitle,
            int plannedMinutes,
            String status
    ) {
    }

    /** '오늘 할 일' + 진행률(done/total). */
    public record TodayView(
            List<CalendarItem> items,
            int doneCount,
            int totalCount
    ) {
    }
}
