package com.wanted.backend.domain.community.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "통합 게시판 항목 (게시글 또는 스터디)")
public record UnifiedBoardItemResponse(
        @Schema(description = "항목 타입 (POST: 게시글, STUDY: 스터디)", example = "POST")
        String type,
        @Schema(description = "게시글 ID (POST 타입일 경우)", example = "37")
        Long postId,
        @Schema(description = "스터디 그룹 ID (STUDY 타입일 경우)", example = "101")
        Long groupId,
        @Schema(description = "게시판 타입 (FREE: 자유게시판, QUESTION: 질문게시판, STUDY: 스터디)", example = "FREE")
        String boardType,
        @Schema(description = "제목", example = "Spring Security 질문드립니다.")
        String title,
        @Schema(description = "작성자 이름", example = "홍길동")
        String authorName,
        @Schema(description = "조회수 (POST 타입)", example = "142")
        Integer viewCount,
        @Schema(description = "댓글 수 (POST 타입)", example = "8")
        Integer commentCount,
        @Schema(description = "과목명 (STUDY 타입)", example = "수학1")
        String subjectName,
        @Schema(description = "현재 참여 인원 (STUDY 타입)", example = "3")
        Integer currentCount,
        @Schema(description = "최대 참여 인원 (STUDY 타입)", example = "6")
        Integer maxCount,
        @Schema(description = "모집 마감 여부 (STUDY 타입)", example = "false")
        Boolean isClosed,
        @Schema(description = "채택 완료 여부 (POST 타입, QUESTION 게시판에서만 의미 있음)", example = "true")
        Boolean isAccepted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
        @Schema(description = "작성일시", example = "2025-03-15T14:30:00")
        LocalDateTime createdAt
) {
    public static UnifiedBoardItemResponse fromPost(PostItemResponse post) {
        return fromFields("POST", post.postId(), post.boardType().name(),
                post.title(), post.authorName(), post.viewCount(), post.commentCount(), post.isAccepted(), post.createdAt());
    }

    public static UnifiedBoardItemResponse fromPostItem(PostItemResult result) {
        return fromFields("POST", result.postId(), result.boardType().name(),
                result.title(), result.authorName(), result.viewCount(), result.commentCount(), result.isAccepted(), result.createdAt());
    }

    private static UnifiedBoardItemResponse fromFields(String type, Long postId, String boardType,
                                                       String title, String authorName, Integer viewCount, Integer commentCount,
                                                       Boolean isAccepted, LocalDateTime createdAt) {
        return new UnifiedBoardItemResponse(
                type, postId, null, boardType, title, authorName,
                viewCount, commentCount, null, null, null, null, isAccepted, createdAt);
    }
}