package com.wanted.backend.domain.cource.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "레슨 수정 요청")
public record UpdateLessonRequest(
        @Schema(description = "레슨 ID (신규 레슨이면 필드 생략 또는 null)", example = "1")
        Long lessonId,

        @Schema(description = "레슨 제목", example = "함수의 극한 개념 정리")
        @NotBlank(message = "회차 제목은 필수입니다.")
        String title,

        @Schema(description = "레슨 설명", example = "함수의 극한 기본 개념과 계산법을 정리합니다.")
        String description,

        @Schema(description = "레슨 순서 (0-based)", example = "1")
        @PositiveOrZero(message = "레슨 순서는 0 이상이어야 합니다.")
        int orderIndex,

        @Schema(description = "영상 재생시간 (초, 프론트에서 메타데이터 추출)", example = "765")
        Integer durationSeconds
) {}
