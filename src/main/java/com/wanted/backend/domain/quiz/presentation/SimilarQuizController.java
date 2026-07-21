package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.usecase.SimilarQuizUseCase;
import com.wanted.backend.domain.quiz.application.usecase.SubmitSimilarQuizUseCase;
import com.wanted.backend.domain.quiz.presentation.request.SimilarQuizRequest;
import com.wanted.backend.domain.quiz.presentation.request.SimilarQuizSubmitRequest;
import com.wanted.backend.domain.quiz.presentation.response.SimilarQuizResponse;
import com.wanted.backend.domain.quiz.presentation.response.SimilarQuizSubmitResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/similar-quizzes")
@RequiredArgsConstructor
@Tag(name = "Similar Quiz", description = "오답 기반 유사문제 추천 API (구독 학생)")
public class SimilarQuizController {

    private final SimilarQuizUseCase similarQuizUseCase;
    private final SubmitSimilarQuizUseCase submitSimilarQuizUseCase;

    @PostMapping
    @Operation(summary = "유사퀴즈 생성", description = "강의(course) 오답 기반 유사문제 세트를 생성·영속한다. 구독 회원 전용(403). 오답/유사문제 없으면 data=null(정상 empty).")
    public ResponseEntity<ApiResponse<SimilarQuizResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SimilarQuizRequest request
    ) {
        SimilarQuizResult result = similarQuizUseCase.generateForCourse(
                userDetails.getMemberId(), request.courseId(), request.week());
        SimilarQuizResponse body = result == null ? null : SimilarQuizResponse.from(result);
        return ApiResponse.success("유사퀴즈 조회 성공", body);
    }

    @PostMapping("/{similarQuizId}/submit")
    @Operation(summary = "유사퀴즈 제출·채점", description = "답안을 채점하고 정답·내답·해설을 포함해 반환한다(해설 화면 바로 렌더). 구독 회원·본인 세트만 가능.")
    public ResponseEntity<ApiResponse<SimilarQuizSubmitResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long similarQuizId,
            @Valid @RequestBody(required = false) SimilarQuizSubmitRequest request
    ) {
        List<SubmitSimilarQuizCommand.AnswerCommand> answers =
                request == null || request.answers() == null ? List.of()
                        : request.answers().stream()
                                .map(a -> new SubmitSimilarQuizCommand.AnswerCommand(
                                        a.questionId(), a.selectedIndex(), a.timeSpentSeconds()))
                                .toList();

        SimilarQuizSubmissionResult result = submitSimilarQuizUseCase.submit(
                new SubmitSimilarQuizCommand(similarQuizId, userDetails.getMemberId(), answers));

        return ApiResponse.success("유사퀴즈 채점 완료", SimilarQuizSubmitResponse.from(result));
    }
}
