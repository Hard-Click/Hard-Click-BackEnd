package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.port.StudyFeedPort;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedResult;
import com.wanted.backend.domain.community.application.usecase.PostQueryUseCase;
import com.wanted.backend.domain.community.application.usecase.UnifiedBoardQueryUseCase;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 커뮤니티 '전체' 피드 — 게시글(POST)과 스터디 모집글(STUDY)을 합쳐 정렬·페이지네이션한다.
 *
 * <p>게시글 조회는 PostQueryUseCase, 스터디는 StudyFeedPort 로만 접근한다(application 레이어가
 * infrastructure 를 직접 알지 않도록 — 포트-어댑터 경계). 병합/정렬/슬라이스는 {@link UnifiedFeedAssembler}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnifiedBoardQueryService implements UnifiedBoardQueryUseCase {

    private static final int PAGE_SIZE = 10;

    private final PostQueryUseCase postQueryUseCase;
    private final StudyFeedPort studyFeedPort;

    @Override
    public UnifiedFeedResult getUnifiedFeed(PostSortType sort, String keyword, int page, boolean isAdmin, Long memberId) {
        // 게시글 총 개수(페이지 수·totalCount 계산용)는 getList 결과에서 가져온다.
        int postTotal = postQueryUseCase.getList(null, sort, keyword, 0, isAdmin, memberId).totalCount();

        // 스터디를 전역 정렬 기준으로 정확히 끼워 넣으려면, 요청 페이지 윈도를 덮는
        // 상위 (page+1)*PAGE_SIZE 게시글이 필요하다(스터디는 소량이라 전량 로드).
        int limit = (page + 1) * PAGE_SIZE;
        List<PostItemResult> topPosts = postQueryUseCase.getTopForFeed(sort, keyword, limit, isAdmin, memberId);

        return UnifiedFeedAssembler.assemble(
                topPosts, studyFeedPort.findActiveStudies(), sort, page, PAGE_SIZE, postTotal);
    }
}
