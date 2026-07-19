package com.wanted.backend.domain.quiz.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "유사퀴즈 제출 요청")
public record SimilarQuizSubmitRequest(
        @Schema(description = "문항별 답안 목록")
        @Valid
        List<Answer> answers
) {
    @Schema(description = "문항 답안")
    public record Answer(
            @Schema(description = "문항 ID", example = "301")
            @NotNull(message = "questionId는 필수입니다.")
            Long questionId,

            @Schema(description = "선택한 보기 순서(0~3)", example = "2")
            @NotNull(message = "selectedIndex는 필수입니다.")
            @Min(value = 0, message = "selectedIndex는 0 이상이어야 합니다.")
            @Max(value = 3, message = "selectedIndex는 3 이하여야 합니다.")
            Integer selectedIndex
    ) {}
}
