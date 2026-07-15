package com.wanted.backend.domain.study_schedule.presentation.request;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 할 일 추가/수정 요청.
 *
 * <p>시작·종료 시각은 <b>둘 다 넣거나 둘 다 빼야 한다</b>(하나만 넣으면 SC003).
 * 둘 다 빼면 타임테이블에 안 뜨고 오늘 할 일 목록에만 보인다.
 */
@Schema(description = "할 일 추가/수정")
public record SaveTodoRequest(

        @Schema(description = "할 일 제목", example = "지난주 복습 퀴즈")
        @NotBlank(message = "할 일 제목은 필수입니다.")
        @Size(max = 100, message = "할 일 제목은 100자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "과목(캘린더 색상 구분용). 없으면 색상 미지정으로 표시된다.", example = "복습")
        @Size(max = 30, message = "과목은 30자를 초과할 수 없습니다.")
        String subject,

        @Schema(description = "할 일 날짜(ISO)", example = "2026-07-15")
        @NotNull(message = "날짜는 필수입니다.")
        LocalDate planDate,

        @Schema(description = "시작 시각(HH:mm). endTime 과 함께 넣거나 함께 빼야 한다.", example = "20:00", type = "string")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "종료 시각(HH:mm). startTime 보다 뒤여야 한다.", example = "21:00", type = "string")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime
) {

    public ScheduleDtos.TodoCommand toCommand() {
        return new ScheduleDtos.TodoCommand(title, subject, planDate, startTime, endTime);
    }
}
