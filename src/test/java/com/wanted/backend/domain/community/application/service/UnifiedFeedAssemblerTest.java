package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.port.StudyFeedPort.StudyFeedItem;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedResult;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedFeedAssemblerTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 7, 20, 12, 0);

    private static PostItemResult post(long id, LocalDateTime createdAt, int views, int comments) {
        return new PostItemResult(id, BoardType.FREE, null, "post" + id, "작성자",
                createdAt, views, comments, false);
    }

    private static StudyFeedItem study(long groupId, LocalDateTime createdAt) {
        return new StudyFeedItem(groupId, "study" + groupId, "방장", "수학1", 3, 6, false, createdAt);
    }

    /** latest 정렬: 게시글·스터디가 createdAt 내림차순으로 섞이고, totalCount는 글+스터디 합. */
    @Test
    void latest_interleavesPostsAndStudiesByCreatedAtDesc() {
        List<PostItemResult> posts = List.of(
                post(1, T.plusMinutes(5), 10, 1),
                post(2, T.plusMinutes(3), 10, 1));
        List<StudyFeedItem> studies = List.of(study(100, T.plusMinutes(4)));

        UnifiedFeedResult result =
                UnifiedFeedAssembler.assemble(posts, studies, PostSortType.latest, 0, 10, 2);

        assertThat(result.items()).extracting(UnifiedFeedItemResult::type)
                .containsExactly("POST", "STUDY", "POST");   // t+5, t+4, t+3
        assertThat(result.items()).extracting(UnifiedFeedItemResult::postId)
                .containsExactly(1L, null, 2L);
        assertThat(result.items().get(1).groupId()).isEqualTo(100L);
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    /** views 정렬: 스터디는 조회수 개념이 없어 최신이라도 게시글 뒤로 밀린다(1-A). */
    @Test
    void views_putsStudiesAfterPostsEvenIfNewer() {
        List<PostItemResult> posts = List.of(
                post(1, T.plusMinutes(1), 5, 0),
                post(2, T.plusMinutes(2), 50, 0));
        List<StudyFeedItem> studies = List.of(study(100, T.plusMinutes(9)));  // 가장 최신

        UnifiedFeedResult result =
                UnifiedFeedAssembler.assemble(posts, studies, PostSortType.views, 0, 10, 2);

        assertThat(result.items()).extracting(UnifiedFeedItemResult::type)
                .containsExactly("POST", "POST", "STUDY");
        assertThat(result.items()).extracting(UnifiedFeedItemResult::postId)
                .containsExactly(2L, 1L, null);   // 조회수 50 > 5, 스터디 맨 뒤
    }

    /** comments 정렬도 스터디는 맨 뒤. */
    @Test
    void comments_putsStudiesAfterPostsByCommentCountDesc() {
        List<PostItemResult> posts = List.of(
                post(1, T.plusMinutes(1), 0, 2),
                post(2, T.plusMinutes(2), 0, 9));
        List<StudyFeedItem> studies = List.of(study(100, T.plusMinutes(9)));

        UnifiedFeedResult result =
                UnifiedFeedAssembler.assemble(posts, studies, PostSortType.comments, 0, 10, 2);

        assertThat(result.items()).extracting(UnifiedFeedItemResult::postId)
                .containsExactly(2L, 1L, null);
    }

    /** 페이지 슬라이스: pageSize=2, 스터디 1 + 게시글 3 → 2페이지. 경계에서 누락/중복 없이 나뉜다. */
    @Test
    void paginatesAcrossMergedList() {
        StudyFeedItem s1 = study(100, T.plusMinutes(6));   // 가장 최신
        PostItemResult p1 = post(1, T.plusMinutes(5), 0, 0);
        PostItemResult p2 = post(2, T.plusMinutes(4), 0, 0);
        PostItemResult p3 = post(3, T.plusMinutes(3), 0, 0);

        // page 0: 호출 측이 상위 (0+1)*2=2 게시글만 넘김
        UnifiedFeedResult page0 = UnifiedFeedAssembler.assemble(
                List.of(p1, p2), List.of(s1), PostSortType.latest, 0, 2, 3);
        assertThat(page0.items()).extracting(UnifiedFeedItemResult::type)
                .containsExactly("STUDY", "POST");          // S1(t+6), P1(t+5)
        assertThat(page0.items().get(1).postId()).isEqualTo(1L);
        assertThat(page0.totalCount()).isEqualTo(4);
        assertThat(page0.totalPages()).isEqualTo(2);

        // page 1: 호출 측이 상위 (1+1)*2=4 게시글(전부) 넘김
        UnifiedFeedResult page1 = UnifiedFeedAssembler.assemble(
                List.of(p1, p2, p3), List.of(s1), PostSortType.latest, 1, 2, 3);
        assertThat(page1.items()).extracting(UnifiedFeedItemResult::postId)
                .containsExactly(2L, 3L);                   // P2(t+4), P3(t+3)
        assertThat(page1.totalCount()).isEqualTo(4);
    }

    /** 스터디 항목 필드 매핑 확인. */
    @Test
    void mapsStudyFields() {
        UnifiedFeedResult result = UnifiedFeedAssembler.assemble(
                List.of(), List.of(study(100, T)), PostSortType.latest, 0, 10, 0);

        UnifiedFeedItemResult item = result.items().get(0);
        assertThat(item.type()).isEqualTo("STUDY");
        assertThat(item.groupId()).isEqualTo(100L);
        assertThat(item.boardType()).isEqualTo("STUDY");
        assertThat(item.currentCount()).isEqualTo(3);
        assertThat(item.maxCount()).isEqualTo(6);
        assertThat(item.isClosed()).isFalse();
        assertThat(item.postId()).isNull();
        assertThat(item.viewCount()).isNull();
    }
}
