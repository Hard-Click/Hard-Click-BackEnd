package com.wanted.backend.domain.study.presentation;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.command.JoinStudyCommand;
import com.wanted.backend.domain.study.application.command.LeaveStudyCommand;
import com.wanted.backend.domain.study.application.command.UpdateStudyCommand;
import com.wanted.backend.domain.study.application.result.JoinStudyResult;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.result.StudyDetailResult;
import com.wanted.backend.domain.study.application.result.StudyListResult;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.application.usecase.StudyQueryUseCase;
import com.wanted.backend.domain.study.presentation.request.CreateStudyRequest;
import com.wanted.backend.domain.study.presentation.request.StudyListRequest;
import com.wanted.backend.domain.study.presentation.response.CreateStudyResponse;
import com.wanted.backend.domain.study.presentation.response.JoinStudyResponse;
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

    @Operation(
            summary = "스터디 상세 조회",
            description = """
                스터디 상세 정보를 조회합니다.
                - 연결된 채팅방 ID(chatRoomId)가 함께 반환됩니다.
                - 참여자 이름 목록(members)은 본인이 참여 중인 경우에만 노출됩니다.
                """
    )
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudyDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        StudyDetailResult result = studyQueryUseCase.getDetail(groupId, userDetails.getMemberId());
        return ApiResponse.success("스터디 상세 조회 완료", StudyDetailResponse.from(result));
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

    @Operation(
            summary = "스터디 모집글 수정",
            description = """
                스터디 모집글을 수정합니다.
                - 방장만 수정할 수 있습니다.
                - 정원(maxCount)은 현재 참여 인원 이상이어야 합니다.
                """
    )
    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> updateStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateStudyRequest request) {

        studyCommandUseCase.update(new UpdateStudyCommand(
                groupId,
                userDetails.getMemberId(),
                request.title(),
                request.subject().name(),
                request.maxCount(),
                request.content()
        ));

        return ApiResponse.success("스터디가 수정되었습니다.", null);
    }

    @Operation(
            summary = "스터디 참여",
            description = """
                스터디에 즉시 참여합니다.
                - 승인 절차 없이 즉시 참여 처리되며, 참여와 동시에 채팅방 참여자로도 등록됩니다.
                - 정원이 가득 차면 스터디가 자동으로 마감됩니다.
                """
    )
    @PostMapping("/{groupId}/join")
    public ResponseEntity<ApiResponse<JoinStudyResponse>> joinStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        JoinStudyResult result = studyCommandUseCase.join(new JoinStudyCommand(groupId, userDetails.getMemberId()));

        return ApiResponse.success("스터디에 참여했습니다.", JoinStudyResponse.from(result));
    }

    @Operation(
            summary = "스터디 퇴장",
            description = """
                참여 중인 스터디에서 나갑니다.
                - 퇴장과 동시에 채팅방 참여자 목록에서도 제거됩니다.
                - 방장은 다른 참여자가 남아있는 동안 나갈 수 없습니다.
                """
    )
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        studyCommandUseCase.leave(new LeaveStudyCommand(groupId, userDetails.getMemberId()));

        return ApiResponse.success("스터디에서 퇴장했습니다", null);
    }

    // TODO(#441): mock 삭제(해산) → 실제 로직으로 교체 (다른 참여자 존재 시 403, 채팅방 함께 CLOSED)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId) {

        return ApiResponse.successNoContent("스터디가 삭제되었습니다.");
    }
}
