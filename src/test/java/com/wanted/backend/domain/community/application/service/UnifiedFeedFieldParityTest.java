package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedResult;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import com.wanted.backend.domain.community.presentation.response.PostItemResponse;
import com.wanted.backend.domain.community.presentation.response.UnifiedBoardItemResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 필드 패리티 계약 테스트: 전체 피드(POST 항목)가 개별 게시판 API 응답과 같은 필드를 담는지 못 박는다.
 *
 * <p>배경: 전체 피드가 개별 API 대비 필드를 반복적으로 누락했다(isAccepted, subject). 같은 게시글을
 * 두 경로로 매핑해 공통 필드가 일치하는지 검증해두면, 이후 개별 API에 필드가 추가·변경될 때
 * 전체 피드 매핑 누락을 CI 가 자동으로 잡는다.
 */
class UnifiedFeedFieldParityTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 21, 14, 30);

    @Test
    void unifiedFeedPostItemMatchesIndividualBoardApiFields() {
        // 과목·채택 등 게시글의 모든 공통 필드가 채워진 대표 케이스(QUESTION).
        PostItemResult post = new PostItemResult(
                5L, BoardType.QUESTION, "MATH_1", "안녕하세여 수1 쎈 287번 문제",
                "홍길동", CREATED_AT, 142, 8, true);

        // 개별 게시판 API(/boards/QUESTION/posts) 응답
        PostItemResponse individual = PostItemResponse.from(post);

        // 전체 피드(/boards/posts) 응답 — assemble → fromUnified 로 실제 내려가는 항목
        UnifiedFeedResult feed = UnifiedFeedAssembler.assemble(
                List.of(post), List.of(), PostSortType.latest, 0, 10, 1);
        UnifiedBoardItemResponse unified = UnifiedBoardItemResponse.fromUnified(feed.items().get(0));

        // 두 응답의 공통 필드가 모두 일치해야 한다(STUDY 전용 필드는 대상 아님).
        assertThat(unified.postId()).isEqualTo(individual.postId());
        assertThat(unified.boardType()).isEqualTo(individual.boardType().name());
        assertThat(unified.subjectName()).isEqualTo(individual.subject());
        assertThat(unified.title()).isEqualTo(individual.title());
        assertThat(unified.authorName()).isEqualTo(individual.authorName());
        assertThat(unified.viewCount()).isEqualTo(individual.viewCount());
        assertThat(unified.commentCount()).isEqualTo(individual.commentCount());
        assertThat(unified.isAccepted()).isEqualTo(individual.isAccepted());
        assertThat(unified.createdAt()).isEqualTo(individual.createdAt());
    }

    @Test
    void freeBoardPostHasNullSubjectInBothResponses() {
        PostItemResult free = new PostItemResult(
                7L, BoardType.FREE, null, "자유글", "김철수", CREATED_AT, 10, 2, false);

        PostItemResponse individual = PostItemResponse.from(free);
        UnifiedFeedResult feed = UnifiedFeedAssembler.assemble(
                List.of(free), List.of(), PostSortType.latest, 0, 10, 1);
        UnifiedBoardItemResponse unified = UnifiedBoardItemResponse.fromUnified(feed.items().get(0));

        assertThat(individual.subject()).isNull();
        assertThat(unified.subjectName()).isNull();
    }
}
