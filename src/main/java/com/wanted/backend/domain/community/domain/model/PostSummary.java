package com.wanted.backend.domain.community.domain.model;

import java.time.LocalDateTime;

// 게시글 목록 한 행 — 작성자명/댓글수까지 JOIN 한 번으로 미리 채워서 반환한다 (방법③: JOIN + DTO Projection).
public record PostSummary(
        Long postId,
        BoardType boardType,
        String subject,
        String title,
        String authorName,
        LocalDateTime createdAt,
        int viewCount,
        long commentCount,
        boolean isAccepted
) {}
