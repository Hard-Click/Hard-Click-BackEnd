package com.wanted.backend.domain.study_schedule.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleResponses {

    private ScheduleResponses() {
    }

    @Schema(description = "스케줄 캘린더 항목. AI가 배치한 강의(LESSON), 학생이 추가한 할 일(TODO), "
            + "복습 예정일에 도달한 복습(REVIEW)이 섞여 내려온다.")
    public record CalendarItemResponse(
            @Schema(description = "항목 출처. LESSON=AI 배치 강의, TODO=학생이 추가한 할 일, REVIEW=코스 복습. "
                    + "LESSON/TODO 는 완료 처리 엔드포인트가 이 값에 따라 갈리고, REVIEW 는 완료 대신 "
                    + "courseId 로 유사퀴즈 화면으로 이동한다.", example = "LESSON")
            ScheduleItemSource source,

            @Schema(description = "항목 ID. source=LESSON 이면 슬롯 ID, TODO 면 할 일 ID.", example = "9012")
            Long itemId,

            @Schema(description = "계획 날짜", example = "2026-07-15") LocalDate planDate,

            @Schema(description = "시작 시각. null 이면 타임테이블에 배치하지 않고 목록에만 노출한다.",
                    example = "07:00", type = "string")
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,

            @Schema(description = "종료 시각(타임테이블 블록 끝). LESSON 은 시작+계획시간으로 계산된 값. "
                    + "startTime 이 null 이면 null.", example = "08:00", type = "string")
            @JsonFormat(pattern = "HH:mm")
            LocalTime endTime,

            @Schema(description = "과목(색상 구분용)", example = "영어") String subject,

            @Schema(description = "화면 노출 제목. LESSON=레슨명, TODO=사용자가 적은 제목", example = "Unit 3 듣기")
            String title,

            @Schema(description = "계획 학습 시간(분)", example = "60") int plannedMinutes,

            @Schema(description = "상태. LESSON=PLANNED/DONE/MISSED, TODO=PLANNED/DONE", example = "PLANNED")
            String status,

            @Schema(description = "수강 ID. source=LESSON 만 채워짐(TODO/REVIEW 는 null)", example = "1063") Long enrollmentId,
            @Schema(description = "강의 ID. LESSON·REVIEW 는 채워지고 TODO 는 null. REVIEW 는 이 값으로 유사퀴즈로 이동한다.",
                    example = "42") Long courseId,
            @Schema(description = "강의명. LESSON·REVIEW 는 채워지고 TODO 는 null", example = "수능 영어 실전") String courseTitle,
            @Schema(description = "레슨 ID. source=TODO 면 null", example = "301") Long lessonId,
            @Schema(description = "레슨명. source=TODO 면 null", example = "Unit 3 듣기") String lessonTitle
    ) {
        public static CalendarItemResponse from(ScheduleDtos.CalendarItem item) {
            return new CalendarItemResponse(
                    item.source(), item.itemId(), item.planDate(),
                    item.startTime(), item.endTime(),
                    item.subject(), item.title(), item.plannedMinutes(), item.status(),
                    item.enrollmentId(), item.courseId(), item.courseTitle(),
                    item.lessonId(), item.lessonTitle());
        }
    }

    @Schema(description = "오늘 할 일 + 진행률. AI 강의와 학생 할 일을 합쳐서 센다.")
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
