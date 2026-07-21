package com.wanted.backend.domain.community.application.result;

import java.time.LocalDateTime;

/**
 * 전체 피드(게시글 + 스터디) 통합 항목. type 으로 POST/STUDY 를 구분하며,
 * 소스에 따라 채워지는 필드가 다르다(POST: viewCount/commentCount/isAccepted, STUDY: groupId/정원/isClosed).
 */
public record UnifiedFeedItemResult(
        String type,
        Long postId,
        Long groupId,
        String boardType,
        String title,
        String authorName,
        Integer viewCount,
        Integer commentCount,
        String subjectName,
        Integer currentCount,
        Integer maxCount,
        Boolean isClosed,
        Boolean isAccepted,
        LocalDateTime createdAt
) {}
