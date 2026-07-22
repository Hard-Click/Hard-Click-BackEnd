package com.wanted.backend.domain.cource.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Schema(description = "섹션 수정 요청")
public record UpdateSectionRequest(
        @Schema(description = "섹션 ID (신규 섹션이면 필드 생략 또는 null)", example = "1")
        Long sectionId,

        @Schema(description = "섹션 제목", example = "섹션 1: 함수의 극한")
        @NotBlank(message = "섹션 제목은 필수입니다.")
        String title,

        @Schema(description = "섹션 순서 (0-based)", example = "0")
        @PositiveOrZero(message = "섹션 순서는 0 이상이어야 합니다.")
        int orderIndex,

        @Schema(description = "레슨 목록")
        @Valid
        List<UpdateLessonRequest> lessons
) {}
