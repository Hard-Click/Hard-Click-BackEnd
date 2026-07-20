package com.wanted.backend.domain.community.presentation;

import com.wanted.backend.domain.community.application.command.CreatePostCommand;
import com.wanted.backend.domain.community.application.command.DeletePostCommand;
import com.wanted.backend.domain.community.application.command.UpdatePostCommand;
import com.wanted.backend.domain.community.application.result.PostDetailResult;
import com.wanted.backend.domain.community.application.result.PostListResult;
import com.wanted.backend.domain.community.application.usecase.PostCommandUseCase;
import com.wanted.backend.domain.community.application.usecase.PostQueryUseCase;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import com.wanted.backend.domain.community.presentation.request.CreatePostRequest;
import com.wanted.backend.domain.community.presentation.request.UpdatePostRequest;
import com.wanted.backend.domain.community.presentation.response.*;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Community Post", description = "커뮤니티 게시글 API")
@RestController
@RequestMapping("/api")
public class PostController {

    private final PostCommandUseCase postCommandUseCase;
    private final PostQueryUseCase postQueryUseCase;

    public PostController(PostCommandUseCase postCommandUseCase,
                          PostQueryUseCase postQueryUseCase) {
        this.postCommandUseCase = postCommandUseCase;
        this.postQueryUseCase = postQueryUseCase;
    }


    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시글 작성",
            description = """
                게시글을 작성합니다.
                - 로그인한 회원만 작성할 수 있습니다.
                - boardType: FREE(자유게시판), QUESTION(질문게시판)
                - 질문게시판 작성 시 subjectId(강의 ID)를 함께 전달해야 합니다.
                - 제목은 300자 이하, 내용은 필수입니다.
                - 이미지 파일 첨부는 선택사항이며 jpg, jpeg, png만 허용합니다.
                - 요청 타입은 multipart/form-data 입니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "커뮤니티 이용 제한 상태")
    })
    public ResponseEntity<ApiResponse<CreatePostResponse>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("data") @Valid CreatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Long postId = postCommandUseCase.create(new CreatePostCommand(
                userDetails.getMemberId(),
                request.boardType(),
                request.subject() != null ? request.subject().name() : null,
                request.title(),
                request.content(),
                files
        ));

        return ApiResponse.created("게시글이 등록되었습니다.", new CreatePostResponse(postId));
    }


    @GetMapping("/boards/{boardType}/posts")
    @Operation(
            summary = "게시판별 게시글 목록 조회",
            description = """
                특정 게시판의 게시글 목록을 조회합니다.
                - 로그인한 회원만 조회 가능합니다.
                - boardType: FREE(자유게시판), QUESTION(질문게시판)
                - 정렬 기준: latest(최신순), views(조회수순), comments(댓글수순) — 기본값: latest
                - keyword로 제목 검색이 가능합니다. (선택사항)
                - 페이지 기본값: 0
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<PostListResponse>> getPostList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "게시판 타입 (FREE: 자유게시판, QUESTION: 질문게시판)", example = "FREE")
            @PathVariable(required = false) BoardType boardType,
            @Parameter(description = "정렬 기준 (latest: 최신순, views: 조회수순, comments: 댓글수순)", example = "latest")
            @RequestParam(defaultValue = "latest") PostSortType sort,
            @Parameter(description = "제목 검색 키워드", example = "Spring Security")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page) {

        boolean isAdmin = "ADMIN".equals(userDetails.getRole());
        PostListResult result = postQueryUseCase.getList(boardType, sort, keyword, page, isAdmin, userDetails.getMemberId());
        return ApiResponse.success("게시글 목록 조회 성공", PostListResponse.from(result));
    }



    @GetMapping("/boards/posts")
    @Operation(
            summary = "전체 게시글 목록 조회",
            description = """
                게시판 구분 없이 전체 게시글 목록을 조회합니다.
                - 로그인한 회원만 조회 가능합니다.
                - 정렬 기준: latest(최신순), views(조회수순), comments(댓글수순) — 기본값: latest
                - keyword로 제목 검색이 가능합니다. (선택사항)
                - 페이지 기본값: 0
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 게시글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<UnifiedBoardListResponse>> getAllPostList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "정렬 기준 (latest: 최신순, views: 조회수순, comments: 댓글수순)", example = "latest")
            @RequestParam(defaultValue = "latest") PostSortType sort,
            @Parameter(description = "제목 검색 키워드", example = "Spring Security")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page) {

        boolean isAdmin = "ADMIN".equals(userDetails.getRole());
        PostListResult result = postQueryUseCase.getList(null, sort, keyword, page, isAdmin, userDetails.getMemberId());
        List<UnifiedBoardItemResponse> items = result.posts().stream()
                .map(UnifiedBoardItemResponse::fromPostItem)
                .toList();
        return ApiResponse.success("게시글 목록 조회 성공",
                new UnifiedBoardListResponse(items, result.currentPage(), result.totalPages(), result.totalCount()));
    }

    @GetMapping("/posts/{postId}")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                게시글 상세 내용을 조회합니다.
                - 로그인한 회원만 조회 가능합니다.
                - 조회 시 조회수가 1 증가합니다.
                - 본인이 작성한 게시글은 isMyPost: true로 표시됩니다.
                - 채택된 게시글은 isAccepted: true로 표시됩니다.
                - 첨부 파일 URL 목록을 함께 반환합니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 게시글 ID", example = "37")
            @PathVariable Long postId) {

        boolean isAdmin = "ADMIN".equals(userDetails.getRole());
        PostDetailResult result = postQueryUseCase.getDetail(postId, userDetails.getMemberId(), isAdmin);
        return ApiResponse.success("게시글 상세 조회 성공", PostDetailResponse.from(result));
    }


    @PatchMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시글 수정",
            description = """
                본인이 작성한 게시글을 수정합니다.
                - 로그인한 회원만 수정할 수 있습니다.
                - 본인이 작성한 게시글인지 검증 후 수정합니다.
                - 제목은 300자 이하, 내용은 필수입니다.
                - 이미지 파일 첨부는 선택사항이며 jpg, jpeg, png만 허용합니다.
                - 요청 타입은 multipart/form-data 입니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 작성한 게시글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<UpdatePostResponse>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "수정할 게시글 ID", example = "37")
            @PathVariable Long postId,
            @RequestPart("data") @Valid UpdatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Long updatedPostId = postCommandUseCase.update(new UpdatePostCommand(
                userDetails.getMemberId(), postId,
                request.subject() != null ? request.subject().name() : null,
                request.title(),
                request.content(),
                files
        ));

        return ApiResponse.success("게시글이 수정되었습니다.", new UpdatePostResponse(updatedPostId));
    }


    @DeleteMapping("/posts/{postId}")
    @Operation(
            summary = "게시글 삭제",
            description = """
            게시글을 삭제합니다.
            - 로그인한 회원만 삭제할 수 있습니다.
            - 본인이 작성한 게시글인지 검증 후 삭제합니다.
            - ADMIN은 모든 게시글을 삭제할 수 있습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 작성한 게시글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 게시글 ID", example = "37")
            @PathVariable Long postId) {

        postCommandUseCase.delete(new DeletePostCommand(
                userDetails.getMemberId(),
                postId,
                "ADMIN".equals(userDetails.getRole())));

        return ApiResponse.successNoContent("게시글이 삭제되었습니다.");
    }
}