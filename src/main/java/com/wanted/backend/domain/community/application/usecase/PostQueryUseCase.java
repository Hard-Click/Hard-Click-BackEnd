package com.wanted.backend.domain.community.application.usecase;

import com.wanted.backend.domain.community.application.result.PostDetailResult;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.application.result.PostListResult;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.PostSortType;

import java.util.List;

public interface PostQueryUseCase {

    // 게시글 목록 조회
    PostListResult getList(BoardType boardType, PostSortType sort, String keyword, int page, boolean isAdmin, Long memberId);
    PostDetailResult getDetail(Long postId, Long memberId, boolean isAdmin);

    // 전체 피드 병합용: 게시판 구분 없이 상위 limit 개 게시글만 조회(페이지네이션은 호출 측이 병합 후 처리).
    List<PostItemResult> getTopForFeed(PostSortType sort, String keyword, int limit, boolean isAdmin, Long memberId);
}