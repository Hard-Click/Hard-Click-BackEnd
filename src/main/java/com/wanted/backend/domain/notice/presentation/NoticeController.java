package com.wanted.backend.domain.notice.presentation;

import com.wanted.backend.domain.notice.application.command.*;
import com.wanted.backend.domain.notice.application.result.NoticeDetailResult;
import com.wanted.backend.domain.notice.application.result.NoticeListResult;
import com.wanted.backend.domain.notice.application.usecase.NoticeCommandUseCase;
import com.wanted.backend.domain.notice.application.usecase.NoticeQueryUseCase;
import com.wanted.backend.domain.notice.presentation.request.CreateGlobalNoticeRequest;
import com.wanted.backend.domain.notice.presentation.request.CreateNoticeRequest;
import com.wanted.backend.domain.notice.presentation.request.GetNoticeListRequest;
import com.wanted.backend.domain.notice.presentation.request.UpdateNoticeRequest;
import com.wanted.backend.domain.notice.presentation.response.CreateNoticeResponse;
import com.wanted.backend.domain.notice.presentation.response.NoticeDetailResponse;
import com.wanted.backend.domain.notice.presentation.response.NoticeListResponse;
import com.wanted.backend.domain.notice.presentation.response.UpdateNoticeResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Notice", description = "공지사항 API")
@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeCommandUseCase noticeCommandUseCase;
    private final NoticeQueryUseCase noticeQueryUseCase;

    public NoticeController(NoticeCommandUseCase noticeCommandUseCase, NoticeQueryUseCase noticeQueryUseCase) {
        this.noticeCommandUseCase = noticeCommandUseCase;
        this.noticeQueryUseCase = noticeQueryUseCase;
    }


    @PostMapping("/courses/{courseId}/notices")
    @Operation(
            summary = "강의 공지사항 작성",
            description = """
                강사가 특정 강의의 공지사항을 작성합니다.
                - INSTRUCTOR 권한을 가진 회원만 작성할 수 있습니다.
                - 본인이 등록한 강의에만 공지사항을 작성할 수 있습니다.
                - 제목은 200자 이하여야 합니다.
                - isPinned: true 설정 시 상단에 고정됩니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "강의 공지사항 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "INSTRUCTOR 권한 없음 또는 본인 강의 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<CreateNoticeResponse>> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "공지사항을 작성할 강의 ID", example = "5")
            @PathVariable Long courseId,
            @Valid @RequestBody CreateNoticeRequest request) {

        Long noticeId = noticeCommandUseCase.create(new CreateNoticeCommand(
                userDetails.getMemberId(),
                courseId,
                request.title(),
                request.content(),
                request.isPinned()
        ));

        return ApiResponse.created("강의 공지사항 작성 완료", new CreateNoticeResponse(noticeId));
    }


    @PostMapping("/notices")
    @Operation(
            summary = "전체 공지사항 작성",
            description = """
                관리자가 전체 공지사항을 작성합니다.
                - ADMIN 권한을 가진 회원만 작성할 수 있습니다.
                - 제목은 200자 이하여야 합니다.
                - isPinned: true 설정 시 상단에 고정됩니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "전체 공지사항 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    public ResponseEntity<ApiResponse<CreateNoticeResponse>> createGlobalNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateGlobalNoticeRequest request) {

        Long noticeId = noticeCommandUseCase.createGlobal(new CreateGlobalNoticeCommand(
                userDetails.getMemberId(),
                request.title(),
                request.content(),
                request.isPinned()
        ));

        return ApiResponse.created("전체 공지사항 작성 완료", new CreateNoticeResponse(noticeId));
    }

    @GetMapping("/notices")
    @Operation(
            summary = "공지사항 목록 조회",
            description = """
                공지사항 목록을 조회합니다. 강의 상세(GET /api/courses/{id})와 동일하게 공개 API입니다.
                - type으로 조회 범위를 구분합니다. (GLOBAL: 전체 공지, COURSE: 강의 공지)
                - COURSE 타입 + courseId 지정 시: 로그인/수강 여부와 무관하게 누구나 조회 가능합니다.
                - COURSE 타입 + courseId 미지정 시: "내가 속한 강의 공지 모아보기"이므로 로그인이 필요합니다.
                  (ADMIN은 전체 강의, INSTRUCTOR는 본인 담당 강의, STUDENT는 수강 중인 강의 기준)
                - keyword로 제목 검색이 가능합니다. (선택사항)
                - 페이지 기본값: 0, 사이즈 기본값: 10
                - 상단 고정 공지사항이 우선 노출됩니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COURSE 타입인데 courseId 미전달"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "비로그인 상태로 courseId 없이 COURSE 목록 조회")
    })
    public ResponseEntity<ApiResponse<NoticeListResponse>> getNotices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute GetNoticeListRequest request) {

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        String role = userDetails != null ? userDetails.getRole() : null;

        NoticeListResult result = noticeQueryUseCase.getList(
                new GetNoticeListCommand(request.getType(), request.getCourseId(), request.getKeyword(),
                        request.getPage(), request.getSize(), memberId, role));

        return ApiResponse.success("공지사항 목록 조회 성공", NoticeListResponse.from(result));
    }

    @GetMapping("/notices/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = """
        공지사항 상세 내용을 조회합니다. 강의 상세와 동일하게 공개 API이며, 로그인/수강 여부와 무관하게 누구나 조회 가능합니다.
        - 이전 공지사항 ID와 제목을 함께 반환합니다.
        """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 공지사항 ID", example = "5")
            @PathVariable Long noticeId) {

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        String role = userDetails != null ? userDetails.getRole() : null;

        NoticeDetailResult result = noticeQueryUseCase.getDetail(noticeId, memberId, role);

        return ApiResponse.success("공지사항 상세 조회 성공", NoticeDetailResponse.from(result));
    }

    @PatchMapping("/notices/{noticeId}")
    @Operation(
            summary = "공지사항 수정",
            description = """
                작성한 공지사항을 수정합니다.
                - 로그인한 회원만 수정할 수 있습니다.
                - 본인이 작성한 공지사항인지 검증 후 수정합니다.
                - 제목은 200자 이하여야 합니다.
                - isPinned 값을 변경하여 상단 고정 여부를 수정할 수 있습니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 작성한 공지사항이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<UpdateNoticeResponse>> updateNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "수정할 공지사항 ID", example = "5")
            @PathVariable Long noticeId,
            @Valid @RequestBody UpdateNoticeRequest request) {

        noticeCommandUseCase.update(new UpdateNoticeCommand(
                userDetails.getMemberId(),
                noticeId,
                request.title(),
                request.content(),
                request.isPinned()
        ));

        return ApiResponse.success("공지사항이 수정되었습니다.", new UpdateNoticeResponse(noticeId));
    }

    @DeleteMapping("/notices/{noticeId}")
    @Operation(
            summary = "공지사항 삭제",
            description = """
                작성한 공지사항을 삭제합니다.
                - 로그인한 회원만 삭제할 수 있습니다.
                - 본인이 작성한 공지사항인지 검증 후 삭제합니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "공지사항 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 작성한 공지사항이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 공지사항 ID", example = "5")
            @PathVariable Long noticeId) {

        noticeCommandUseCase.delete(new DeleteNoticeCommand(
                userDetails.getMemberId(),
                noticeId
        ));

        return ApiResponse.successNoContent("공지사항이 삭제되었습니다.");
    }
}