package com.wanted.backend.domain.study_schedule.presentation;

import com.wanted.backend.domain.study_schedule.application.usecase.StudentTodoUseCase;
import com.wanted.backend.domain.study_schedule.presentation.request.SaveTodoRequest;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학습 스케줄 화면의 '+ 할 일 추가' API.
 *
 * <p>추가한 할 일은 AI 슬롯과 함께 GET /api/schedule/me, /me/today 에 합쳐져 내려간다
 * (source=TODO 로 구분). AI 리플로우가 이 항목을 건드리지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule/todos")
@Tag(name = "Study Schedule", description = "학습 스케줄 API (학생)")
public class StudentTodoController {

    private final StudentTodoUseCase studentTodoUseCase;

    @Operation(summary = "할 일 추가",
            description = "학습 스케줄에 직접 할 일을 추가합니다. 시작·종료 시각을 넣으면 타임테이블에도 배치됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "시각을 하나만 입력(SC003) 또는 종료가 시작보다 앞섬(SC004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody SaveTodoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long todoId = studentTodoUseCase.create(userDetails.getMemberId(), request.toCommand());
        return ApiResponse.created("할 일이 추가되었습니다.", todoId);
    }

    @Operation(summary = "할 일 수정", description = "본인 할 일의 제목·과목·날짜·시간을 수정합니다. 완료 상태는 바뀌지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "시각 입력 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 소유")
    })
    @PutMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "할 일 ID", example = "5") @PathVariable Long todoId,
            @Valid @RequestBody SaveTodoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studentTodoUseCase.update(userDetails.getMemberId(), todoId, request.toCommand());
        return ApiResponse.successNoContent("할 일이 수정되었습니다.");
    }

    @Operation(summary = "할 일 삭제", description = "본인 할 일을 삭제합니다. AI가 배치한 강의 슬롯은 삭제할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 소유")
    })
    @DeleteMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "할 일 ID", example = "5") @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studentTodoUseCase.delete(userDetails.getMemberId(), todoId);
        return ApiResponse.successNoContent("할 일이 삭제되었습니다.");
    }

    @Operation(summary = "할 일 완료 체크", description = "본인 할 일을 완료(DONE) 처리합니다. 오늘 진행률에 반영됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 소유")
    })
    @PatchMapping("/{todoId}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(
            @Parameter(description = "할 일 ID", example = "5") @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studentTodoUseCase.complete(userDetails.getMemberId(), todoId);
        return ApiResponse.successNoContent("완료 처리되었습니다.");
    }

    @Operation(summary = "할 일 완료 취소",
            description = "본인 할 일의 완료를 취소(PLANNED)합니다. 실수 체크·마음 변경 시 되돌립니다. "
                    + "AI 학습 슬롯(LESSON)은 대상이 아니며 TODO에만 적용됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 소유")
    })
    @PatchMapping("/{todoId}/incomplete")
    public ResponseEntity<ApiResponse<Void>> incomplete(
            @Parameter(description = "할 일 ID", example = "5") @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studentTodoUseCase.incomplete(userDetails.getMemberId(), todoId);
        return ApiResponse.successNoContent("완료가 취소되었습니다.");
    }
}
