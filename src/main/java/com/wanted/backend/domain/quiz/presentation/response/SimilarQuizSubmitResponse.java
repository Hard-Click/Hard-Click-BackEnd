package com.wanted.backend.domain.quiz.presentation.response;

import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "유사퀴즈 제출·채점 응답 — 정답/내답/해설 포함(해설 화면 바로 렌더)")
public record SimilarQuizSubmitResponse(
        @Schema(description = "유사퀴즈 ID", example = "123") Long similarQuizId,
        @Schema(description = "점수(100점 만점)", example = "60") int score,
        @Schema(description = "맞힌 문항 수", example = "3") int correctCount,
        @Schema(description = "전체 문항 수", example = "5") int totalCount,
        @Schema(description = "문항별 채점 결과") List<QuestionResult> questions
) {
    @Schema(description = "문항 채점 결과")
    public record QuestionResult(
            @Schema(description = "문항 ID", example = "301") Long questionId,
            @Schema(description = "문항 내용") String content,
            @Schema(description = "보기 목록(1번부터 순서대로)") List<String> options,
            @Schema(description = "정답 보기 순서(0-based)", example = "1") int answerIndex,
            @Schema(description = "내가 고른 보기 순서(0-based), 미응답 시 null", example = "2") Integer selectedIndex,
            @Schema(description = "해설") String explanation,
            @Schema(description = "정답 여부", example = "false") boolean correct
    ) {}

    public static SimilarQuizSubmitResponse from(SimilarQuizSubmissionResult result) {
        return new SimilarQuizSubmitResponse(
                result.similarQuizId(),
                result.score(),
                result.correctCount(),
                result.totalCount(),
                result.questions().stream()
                        .map(question -> new QuestionResult(
                                question.questionId(),
                                question.content(),
                                question.options(),
                                question.answerIndex(),
                                question.selectedIndex(),
                                question.explanation(),
                                question.correct()))
                        .toList()
        );
    }
}
