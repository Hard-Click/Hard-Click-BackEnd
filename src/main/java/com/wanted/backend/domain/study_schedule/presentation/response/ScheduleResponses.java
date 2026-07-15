package com.wanted.backend.domain.study_schedule.presentation.response;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleResponses {

    private ScheduleResponses() {
    }

    @Schema(description = "스케줄 캘린더 항목")
    public record CalendarItemResponse(
            @Schema(description = "슬롯 ID", example = "9012") Long slotId,
            @Schema(description = "계획 날짜", example = "2026-07-14") LocalDate planDate,
            @Schema(description = "시작 시각(없을 수 있음)", example = "19:00") LocalTime startTime,
            @Schema(description = "수강 ID", example = "1063") Long enrollmentId,
            @Schema(description = "강의 ID", example = "42") Long courseId,
            @Schema(description = "과목(색상 구분용)", example = "영어") String subject,
            @Schema(description = "강의명", example = "수능 영어 실전") String courseTitle,
            @Schema(description = "레슨 ID", example = "301") Long lessonId,
            @Schema(description = "레슨명", example = "Unit 3 듣기") String lessonTitle,
            @Schema(description = "계획 학습 시간(분)", example = "40") int plannedMinutes,
            @Schema(description = "상태(PLANNED/DONE/MISSED)", example = "PLANNED") String status
    ) {
        public static CalendarItemResponse from(ScheduleDtos.CalendarItem item) {
            return new CalendarItemResponse(
                    item.slotId(), item.planDate(), item.startTime(),
                    item.enrollmentId(), item.courseId(), item.subject(), item.courseTitle(),
                    item.lessonId(), item.lessonTitle(), item.plannedMinutes(), item.status());
        }
    }

    @Schema(description = "오늘 할 일 + 진행률")
    public record TodayResponse(
            @Schema(description = "오늘 할 일 목록") List<CalendarItemResponse> items,
            @Schema(description = "완료 수", example = "1") int doneCount,
            @Schema(description = "전체 수", example = "3") int totalCount
    ) {
        public static TodayResponse from(ScheduleDtos.TodayView view) {
            return new TodayResponse(
                    view.items().stream().map(CalendarItemResponse::from).toList(),
                    view.doneCount(),
                    view.totalCount());
        }
    }
}
