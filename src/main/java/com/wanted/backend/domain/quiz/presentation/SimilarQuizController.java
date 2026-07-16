package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;
import com.wanted.backend.domain.quiz.application.usecase.SimilarQuizUseCase;
import com.wanted.backend.domain.quiz.presentation.request.SimilarQuizRequest;
import com.wanted.backend.domain.quiz.presentation.response.SimilarQuizResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/similar-quizzes")
@RequiredArgsConstructor
@Tag(name = "Similar Quiz", description = "오답 기반 유사문제 추천 API (학생)")
public class SimilarQuizController {

    private final SimilarQuizUseCase similarQuizUseCase;

    @PostMapping
    @Operation(summary = "유사퀴즈 생성", description = "강의(course) 오답 기반 유사문제 세트를 생성한다. 오답/유사문제 없으면 data=null(정상 empty).")
    public ResponseEntity<ApiResponse<SimilarQuizResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SimilarQuizRequest request
    ) {
        SimilarQuizResult result = similarQuizUseCase.generateForCourse(userDetails.getMemberId(), request.courseId());
        SimilarQuizResponse body = result == null ? null : SimilarQuizResponse.from(result);
        return ApiResponse.success("유사퀴즈 조회 성공", body);
    }
}
