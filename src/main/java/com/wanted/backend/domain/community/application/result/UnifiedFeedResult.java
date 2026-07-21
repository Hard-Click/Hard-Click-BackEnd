package com.wanted.backend.domain.community.application.result;

import java.util.List;

public record UnifiedFeedResult(
        List<UnifiedFeedItemResult> items,
        int currentPage,
        int totalPages,
        long totalCount
) {}
