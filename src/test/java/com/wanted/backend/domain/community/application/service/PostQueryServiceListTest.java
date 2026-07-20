package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.application.port.MemberNamePort;
import com.wanted.backend.domain.community.application.result.PostListResult;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import com.wanted.backend.domain.community.domain.model.PostSummary;
import com.wanted.backend.domain.community.domain.repository.CommentRepository;
import com.wanted.backend.domain.community.domain.repository.PostFileRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.community.domain.repository.ViewLogRepository;
import com.wanted.backend.domain.community.infrastructure.cache.PostCountCache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

// 목록 응답의 isAccepted 노출(#599) — 상세에만 있던 채택 상태가 두 목록 경로
// (Batch IN / PostSummary 프로젝션) 모두에서 내려오는지 검증한다.
@ExtendWith(MockitoExtension.class)
class PostQueryServiceListTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostFileRepository postFileRepository;

    @Mock
    private ViewLogRepository viewLogRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommunityFileStoragePort fileStoragePort;

    @Mock
    private CommunityAccessPolicy communityAccessPolicy;

    @Mock
    private PostCountCache postCountCache;

    private PostQueryService postQueryService;

    @BeforeEach
    void setUp() {
        postQueryService = new PostQueryService(postRepository, postFileRepository, viewLogRepository,
                memberNamePort, commentRepository, fileStoragePort, new SimpleMeterRegistry(),
                communityAccessPolicy, postCountCache);

        lenient().when(postCountCache.count(any(), any())).thenReturn(1);
        lenient().when(memberNamePort.getNamesByMemberIds(anyCollection())).thenReturn(Map.of(2L, "김민수"));
        lenient().when(commentRepository.countsByPostIds(anyList())).thenReturn(Map.of(37L, 3L));
    }

    private Post questionPost(boolean isAccepted) {
        return Post.restore(37L, 2L, BoardType.QUESTION, "MATH_1",
                "Spring Security 질문", "내용", 10, isAccepted,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("채택 완료된 질문글은 목록에서 isAccepted=true로 내려간다")
    void getList_acceptedQuestion_true() {
        given(postRepository.findByBoardType(BoardType.QUESTION, PostSortType.latest, null, 0, 10))
                .willReturn(List.of(questionPost(true)));

        PostListResult result = postQueryService.getList(BoardType.QUESTION, PostSortType.latest, null, 0, false, 1L);

        assertThat(result.posts().get(0).isAccepted()).isTrue();
    }

    @Test
    @DisplayName("미채택 질문글은 목록에서 isAccepted=false다")
    void getList_unacceptedQuestion_false() {
        given(postRepository.findByBoardType(BoardType.QUESTION, PostSortType.latest, null, 0, 10))
                .willReturn(List.of(questionPost(false)));

        PostListResult result = postQueryService.getList(BoardType.QUESTION, PostSortType.latest, null, 0, false, 1L);

        assertThat(result.posts().get(0).isAccepted()).isFalse();
    }

    @Test
    @DisplayName("자유게시판 글은 채택 개념이 없어 isAccepted=false다")
    void getList_freePost_false() {
        Post free = Post.restore(38L, 2L, BoardType.FREE, null,
                "자유글", "내용", 5, false, LocalDateTime.now(), LocalDateTime.now());
        given(postRepository.findByBoardType(BoardType.FREE, PostSortType.latest, null, 0, 10))
                .willReturn(List.of(free));
        lenient().when(commentRepository.countsByPostIds(anyList())).thenReturn(Map.of(38L, 0L));

        PostListResult result = postQueryService.getList(BoardType.FREE, PostSortType.latest, null, 0, false, 1L);

        assertThat(result.posts().get(0).isAccepted()).isFalse();
    }

    @Test
    @DisplayName("댓글순 정렬(PostSummary 경로)에서도 isAccepted가 내려간다")
    void getList_commentSort_carriesAccepted() {
        PostSummary summary = new PostSummary(37L, BoardType.QUESTION, "MATH_1",
                "Spring Security 질문", "김민수", LocalDateTime.now(), 10, 3L, true);
        given(postRepository.findAllSummaryOrderByCommentCountDenormalized(null, 0, 10))
                .willReturn(List.of(summary));

        PostListResult result = postQueryService.getList(null, PostSortType.comments, null, 0, false, 1L);

        assertThat(result.posts().get(0).isAccepted()).isTrue();
    }
}
