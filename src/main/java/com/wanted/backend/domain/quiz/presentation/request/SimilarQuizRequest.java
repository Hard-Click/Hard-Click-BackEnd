package com.wanted.backend.domain.quiz.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "유사퀴즈 생성 요청")
public record SimilarQuizRequest(
        @Schema(description = "강의(course) ID", example = "9001")
        @NotNull(message = "courseId는 필수입니다.")
        Long courseId,

        // 진입(캘린더) 주차. 캘린더 확정 전까지 선택 값 — 제목/메타에만 반영한다.
        @Schema(description = "주차(선택, 캘린더 진입 시)", example = "3")
        @Positive(message = "week는 1 이상이어야 합니다.")
        Integer week
) {}
