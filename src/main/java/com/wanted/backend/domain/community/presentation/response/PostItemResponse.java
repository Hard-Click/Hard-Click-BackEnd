package com.wanted.backend.domain.community.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.domain.model.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PostItemResponse(

        @Schema(description = "게시글 ID", example = "37")
        Long postId,

        @Schema(description = "게시판 타입 (FREE: 자유게시판, QUESTION: 질문게시판)", example = "FREE")
        BoardType boardType,

        @Schema(description = "과목 — QUESTION 게시판일 경우 SubjectType enum 값 반환, FREE 게시판은 null",
                example = "MATH_1",
                nullable = true)
        String subject,

        @Schema(description = "게시글 제목", example = "Spring Security 질문드립니다.")
        String title,

        @Schema(description = "작성자 이름", example = "홍길동")
        String authorName,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
        @Schema(description = "작성일시", example = "2025-03-15T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "조회수", example = "142")
        int viewCount,

        @Schema(description = "댓글수", example = "8")
        int commentCount,

        @Schema(description = "채택 완료 여부 — QUESTION 게시판에서만 의미 있음 (FREE는 항상 false)", example = "true")
        boolean isAccepted
) {
        public static PostItemResponse from(PostItemResult result) {
                return new PostItemResponse(
                        result.postId(), result.boardType(), result.subject(),
                        result.title(), result.authorName(), result.createdAt(),
                        result.viewCount(), result.commentCount(), result.isAccepted());
        }
}