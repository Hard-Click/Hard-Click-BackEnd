package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.port.StudyFeedPort.StudyFeedItem;
import com.wanted.backend.domain.community.application.result.PostItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedItemResult;
import com.wanted.backend.domain.community.application.result.UnifiedFeedResult;
import com.wanted.backend.domain.community.domain.model.PostSortType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 전체 피드(게시글 + 스터디)의 병합·정렬·페이지 슬라이스를 담당하는 순수 로직.
 *
 * <p>정렬 정책:
 * <ul>
 *   <li>latest — 게시글·스터디를 createdAt 기준으로 섞는다.
 *   <li>views/comments — 스터디엔 해당 지표가 없으므로 게시글을 지표순으로 먼저, 스터디는 맨 뒤(최신순).
 * </ul>
 *
 * <p>페이징: 호출 측이 상위 {@code (page+1)*pageSize} 개의 게시글 + 전체 활성 스터디를 넘겨주므로,
 * 병합 후 앞에서 {@code [page*pageSize, +pageSize)} 구간만 잘라내면 전역 정렬 기준으로 정확하다
 * (요청 페이지 윈도는 항상 상위 (page+1)*pageSize 안에 들어오기 때문).
 */
final class UnifiedFeedAssembler {

    private static final String TYPE_POST = "POST";
    private static final String TYPE_STUDY = "STUDY";

    private UnifiedFeedAssembler() {
    }

    static UnifiedFeedResult assemble(List<PostItemResult> topPosts, List<StudyFeedItem> studies,
                                      PostSortType sort, int page, int pageSize, int postTotal) {
        List<UnifiedFeedItemResult> merged = new ArrayList<>(topPosts.size() + studies.size());
        for (PostItemResult post : topPosts) {
            merged.add(fromPost(post));
        }
        for (StudyFeedItem study : studies) {
            merged.add(fromStudy(study));
        }
        merged.sort(comparatorFor(sort));

        long totalCount = (long) postTotal + studies.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        int from = Math.min(page * pageSize, merged.size());
        int to = Math.min(from + pageSize, merged.size());
        List<UnifiedFeedItemResult> pageItems = new ArrayList<>(merged.subList(from, to));

        return new UnifiedFeedResult(pageItems, page, totalPages, totalCount);
    }

    private static Comparator<UnifiedFeedItemResult> comparatorFor(PostSortType sort) {
        Comparator<UnifiedFeedItemResult> byCreatedDesc =
                Comparator.comparing(UnifiedFeedItemResult::createdAt, Comparator.reverseOrder());
        if (sort == PostSortType.latest) {
            return byCreatedDesc;
        }
        Function<UnifiedFeedItemResult, Integer> metric = sort == PostSortType.comments
                ? UnifiedFeedItemResult::commentCount
                : UnifiedFeedItemResult::viewCount;
        // 게시글(false)을 스터디(true)보다 앞에, 게시글끼리는 지표 내림차순, 마지막으로 최신순.
        return Comparator.comparing((UnifiedFeedItemResult item) -> TYPE_STUDY.equals(item.type()))
                .thenComparing(metric, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(byCreatedDesc);
    }

    private static UnifiedFeedItemResult fromPost(PostItemResult post) {
        // subjectName: QUESTION 게시글의 과목(PostItemResult.subject, FREE는 null). 개별 게시판 API와 필드 일치.
        return new UnifiedFeedItemResult(
                TYPE_POST, post.postId(), null, post.boardType().name(), post.title(), post.authorName(),
                post.viewCount(), post.commentCount(), post.subject(), null, null, null, post.isAccepted(), post.createdAt());
    }

    private static UnifiedFeedItemResult fromStudy(StudyFeedItem study) {
        return new UnifiedFeedItemResult(
                TYPE_STUDY, null, study.groupId(), TYPE_STUDY, study.title(), study.authorName(),
                null, null, study.subjectName(), study.currentCount(), study.maxCount(),
                study.isClosed(), null, study.createdAt());
    }
}
