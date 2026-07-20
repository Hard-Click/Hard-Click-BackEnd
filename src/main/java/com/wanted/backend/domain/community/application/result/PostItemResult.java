package com.wanted.backend.domain.community.application.result;

import com.wanted.backend.domain.community.domain.model.BoardType;
import java.time.LocalDateTime;

public record PostItemResult(
        Long postId,
        BoardType boardType,
        String subject,
        String title,
        String authorName,
        LocalDateTime createdAt,
        int viewCount,
        int commentCount,
        boolean isAccepted
) {}