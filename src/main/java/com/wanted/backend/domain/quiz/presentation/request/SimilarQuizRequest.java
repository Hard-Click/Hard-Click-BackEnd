package com.wanted.backend.domain.quiz.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "유사퀴즈 생성 요청")
public record SimilarQuizRequest(
        @Schema(description = "강의(course) ID", example = "500")
        @NotNull(message = "courseId는 필수입니다.")
        Long courseId
) {}
