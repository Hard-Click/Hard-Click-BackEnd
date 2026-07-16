package com.wanted.backend.domain.quiz.presentation.response;

import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "유사퀴즈(오답 기반) 응답")
public record SimilarQuizResponse(
        @Schema(description = "유사퀴즈 ID(무상태 생성 시 courseId placeholder)", example = "500") Long similarQuizId,
        @Schema(description = "강의 ID", example = "500") Long courseId,
        @Schema(description = "제목", example = "수능 국어 유사문제") String title,
        @Schema(description = "문항 목록") List<QuestionResponse> questions
) {
    @Schema(description = "유사 문항")
    public record QuestionResponse(
            @Schema(description = "문항 ID", example = "301") Long questionId,
            @Schema(description = "문항 내용") String content,
            @Schema(description = "보기 목록(1번부터 순서대로)") List<String> options
    ) {}

    public static SimilarQuizResponse from(SimilarQuizResult result) {
        return new SimilarQuizResponse(
                result.similarQuizId(),
                result.courseId(),
                result.title(),
                result.questions().stream()
                        .map(question -> new QuestionResponse(
                                question.questionId(), question.content(), question.options()))
                        .toList()
        );
    }
}
