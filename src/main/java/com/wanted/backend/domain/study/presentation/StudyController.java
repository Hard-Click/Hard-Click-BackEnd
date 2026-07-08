package com.wanted.backend.domain.study.presentation;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.application.usecase.StudyQueryUseCase;
import com.wanted.backend.domain.study.presentation.request.CreateStudyRequest;
import com.wanted.backend.domain.study.presentation.request.StudyListRequest;
import com.wanted.backend.domain.study.presentation.response.CreateStudyResponse;
import com.wanted.backend.domain.study.presentation.response.StudyDetailResponse;
import com.wanted.backend.domain.study.presentation.response.StudyListResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/study")
@Validated
public class StudyController {

    private final StudyCommandUseCase studyCommandUseCase;
    private final StudyQueryUseCase studyQueryUseCase;

    public StudyController(StudyCommandUseCase studyCommandUseCase, StudyQueryUseCase studyQueryUseCase) {
        this.studyCommandUseCase = studyCommandUseCase;
        this.studyQueryUseCase = studyQueryUseCase;
    }

    @Operation(summary = "스터디 목록 조회", description = "스터디 목록을 조회합니다. subject로 과목 필터링 가능 (SubjectType enum 값)")
    @GetMapping
    public ResponseEntity<ApiResponse<StudyListResponse>> getStudyList(
            @Valid @ModelAttribute StudyListRequest request) {

        StudyListResult result = studyQueryUseCase.getList(
                request.subject() != null ? request.subject().name() : null,
                request.page(),
                request.size());

        return ApiResponse.success("스터디 목록 조회 완료", StudyListResponse.from(result));
    }

    // TODO(#439): mock 상세 조회 → 실제 조회로 교체 (chatRoomId 포함 예정)
    @Operation(summary = "스터디 상세 조회", description = "스터디 상세 정보를 조회합니다.")
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudyDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        boolean isJoined = true;

        StudyDetailResponse response = new StudyDetailResponse(
                groupId,
                "수학 1등급 목표 스터디",
                "매주 일요일 밤 10시에 모여서 질문 받습니다.",
                "수학1", "이*연", 2, 5, false, isJoined, false,
                isJoined ? List.of("이*연", "김*민") : null,
                LocalDateTime.of(2026, 5, 18, 17, 0)
        );

        return ApiResponse.success("스터디 상세 조회 완료", response);
    }

    @Operation(
            summary = "스터디 모집글 생성",
            description = """
                스터디 모집글을 생성합니다.
                - 로그인한 회원만 작성할 수 있습니다.
                - 생성과 동시에 전용 채팅방이 개설되고, 작성자가 방장 겸 첫 참여자로 등록됩니다.
                - 정원(maxCount)은 최소 2명 이상이어야 합니다.
                """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CreateStudyResponse>> createStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateStudyRequest request) {

        StudyCreationResult result = studyCommandUseCase.create(new CreateStudyCommand(
                userDetails.getMemberId(),
                request.title(),
                request.subject().name(),
                request.maxCount(),
                request.content()
        ));

        return ApiResponse.created("스터디 모집글이 성공적으로 등록되었습니다.",
                new CreateStudyResponse(result.studyId(), result.chatRoomId()));
    }

    // TODO(#440): mock 수정 → 실제 로직으로 교체
    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> updateStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateStudyRequest request) {

        return ApiResponse.success("스터디가 수정되었습니다.", null);
    }

    // TODO(#441): mock 삭제(해산) → 실제 로직으로 교체 (다른 참여자 존재 시 403, 채팅방 함께 CLOSED)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        return ApiResponse.successNoContent("스터디가 삭제되었습니다.");
    }
}
