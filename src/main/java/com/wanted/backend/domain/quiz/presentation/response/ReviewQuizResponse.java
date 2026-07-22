package com.wanted.backend.domain.quiz.presentation.response;

import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "복습 추천 응답('안 B') — 원문제별 유사문제 묶음(정답/해설 제외), 급한 순")
public record ReviewQuizResponse(
        @Schema(description = "복습 그룹 목록(급한 순)") List<ReviewGroupResponse> reviews
) {
    @Schema(description = "원문제 하나 + 그에 붙는 유사문제")
    public record ReviewGroupResponse(
            @Schema(description = "이 그룹의 유사퀴즈 ID(제출 시 사용, 저장 불가 시 null)", example = "123") Long similarQuizId,
            @Schema(description = "원문제 문항 ID", example = "1101") Long originalQuestionId,
            @Schema(description = "섹션 ID", example = "137") Long sectionId,
            @Schema(description = "유사 문항 목록") List<QuestionResponse> similar
    ) {}

    @Schema(description = "유사 문항")
    public record QuestionResponse(
            @Schema(description = "문항 ID", example = "301") Long questionId,
            @Schema(description = "문항 내용") String content,
            @Schema(description = "보기 목록(1번부터 순서대로)") List<String> options
    ) {}

    public static ReviewQuizResponse from(ReviewQuizResult result) {
        return new ReviewQuizResponse(
                result.reviews().stream()
                        .map(group -> new ReviewGroupResponse(
                                group.similarQuizId(),
                                group.originalQuestionId(),
                                group.sectionId(),
                                group.similar().stream()
                                        .map(question -> new QuestionResponse(
                                                question.questionId(), question.content(), question.options()))
                                        .toList()))
                        .toList()
        );
    }
}
