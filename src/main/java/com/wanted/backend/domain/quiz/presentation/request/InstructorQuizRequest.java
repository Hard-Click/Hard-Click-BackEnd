package com.wanted.backend.domain.quiz.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InstructorQuizRequest(
        @NotBlank(message = "퀴즈 제목은 필수입니다.") String quizTitle,
        @NotNull(message = "강의 ID는 필수입니다.") Long courseId,
        @NotNull(message = "섹션 ID는 필수입니다.") Long sectionId,
        @NotEmpty(message = "문항은 최소 1개 이상이어야 합니다.") @Valid List<Question> questions
) {
    public record Question(
            @NotBlank(message = "문제 내용은 필수입니다.") String questionText,
            String explanation,
            @NotNull(message = "난이도는 필수입니다.")
            @Min(value = 1, message = "난이도는 1(하)~3(상) 사이여야 합니다.")
            @Max(value = 3, message = "난이도는 1(하)~3(상) 사이여야 합니다.")
            @Schema(description = "난이도(필수, 1=하/2=중/3=상)", example = "2") Integer difficulty,
            @Min(value = 1, message = "정답 보기 번호는 1~4 사이여야 합니다.")
            @Max(value = 4, message = "정답 보기 번호는 1~4 사이여야 합니다.")
            @Schema(description = "정답 보기 번호(1~4)", example = "2") int correctOptionNumber,
            @Size(min = 4, max = 4, message = "보기는 4개가 필요합니다.") @Valid List<Option> options
    ) {
    }

    public record Option(@NotBlank(message = "보기 내용은 필수입니다.") String optionText) {
    }
}
