package com.wanted.backend.domain.community.application.usecase;

import com.wanted.backend.domain.community.application.result.UnifiedFeedResult;
import com.wanted.backend.domain.community.domain.model.PostSortType;

/**
 * 커뮤니티 '전체' 피드 조회 — 게시글(POST)과 스터디 모집글(STUDY)을 합쳐서 내려준다.
 */
public interface UnifiedBoardQueryUseCase {

    UnifiedFeedResult getUnifiedFeed(PostSortType sort, String keyword, int page, boolean isAdmin, Long memberId);
}
