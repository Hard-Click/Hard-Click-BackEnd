package com.wanted.backend.domain.report_moderation.infrastructure.persistence;

import com.wanted.backend.domain.community.domain.model.Comment;
import com.wanted.backend.domain.community.domain.model.CommentStatus;
import com.wanted.backend.domain.community.domain.model.PostStatus;
import com.wanted.backend.domain.community.domain.model.ReportStatus;
import com.wanted.backend.domain.community.domain.model.ReportType;
import com.wanted.backend.domain.community.domain.model.ReviewStatus;
import com.wanted.backend.domain.community.domain.model.TargetType;
import com.wanted.backend.domain.community.infrastructure.persistence.CommentJpaEntity;
import com.wanted.backend.domain.community.infrastructure.persistence.PostJpaEntity;
import com.wanted.backend.domain.community.infrastructure.persistence.ReportJpaEntity;
import com.wanted.backend.domain.community.infrastructure.persistence.ReviewJpaEntity;
import com.wanted.backend.domain.report_moderation.application.dto.AdminReportDetailResult;
import com.wanted.backend.domain.report_moderation.application.dto.AdminReportListResult;
import com.wanted.backend.domain.report_moderation.application.port.AdminReportQueryPort;
import com.wanted.backend.domain.report_moderation.application.query.AdminReportListQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminReportQueryAdapter implements AdminReportQueryPort {

    private static final int MIN_REPORT_COUNT_FOR_ADMIN_LIST = 3;
    private static final int PREVIEW_LENGTH = 50;
    private static final String DELETED_POST_TITLE = "삭제된 게시글";
    private static final String DELETED_POST_CONTENT = "삭제된 게시글입니다.";
    private static final String COMMENT_TITLE = "댓글 내용";
    private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";
    private static final String REVIEW_TITLE = "리뷰 내용";
    private static final String DELETED_REVIEW_CONTENT = "삭제된 리뷰입니다.";

    private static final String DETAIL_DELETED_POST_CONTENT = "관리자에 의해 삭제된 게시글입니다.";

    private final EntityManager entityManager;

    @Override
    public AdminReportListResult findReports(AdminReportListQuery query) {
        List<ReportTargetGroup> pageGroups = findTargetGroupPage(query);
        int totalElements = countTargetGroups(query);
        int totalPages = calculateTotalPages(totalElements, query.size());

        return new AdminReportListResult(
                pageGroups.stream()
                        .map(this::toItem)
                        .toList(),
                query.page(),
                query.size(),
                totalElements,
                totalPages,
                query.page() + 1 < totalPages
        );
    }

    @Override
    public Optional<AdminReportDetailResult> findReportDetail(Long reportId) {
        ReportJpaEntity requestedReport = entityManager.find(ReportJpaEntity.class, reportId);
        if (requestedReport == null) {
            return Optional.empty();
        }

        List<ReportJpaEntity> reports = entityManager.createQuery("""
                select r
                from ReportJpaEntity r
                where r.targetType = :targetType
                  and r.targetId = :targetId
                order by r.createdAt desc, r.id desc
                """, ReportJpaEntity.class)
                .setParameter("targetType", requestedReport.getTargetType())
                .setParameter("targetId", requestedReport.getTargetId())
                .getResultList();

        if (reports.isEmpty()) {
            return Optional.empty();
        }

        ReportJpaEntity representativeReport = reports.get(0);
        DetailTargetContent targetContent = findDetailTargetContent(
                requestedReport.getTargetType(),
                requestedReport.getTargetId(),
                requestedReport.getReportedMemberId()
        );
        MemberInfo reporter = findMemberInfo(representativeReport.getReporterId());

        return Optional.of(new AdminReportDetailResult(
                representativeReport.getId(),
                requestedReport.getTargetType(),
                requestedReport.getTargetId(),
                targetContent.title(),
                targetContent.content(),
                targetContent.url(),
                targetContent.authorId(),
                findMemberName(targetContent.authorId()),
                reports.size(),
                aggregateReasonCounts(reports),
                representativeReport.getReporterId(),
                reporter.name(),
                reporter.username(),
                normalizeStatus(representativeReport.getStatus()),
                representativeReport.getMemo()
        ));
    }

    private List<AdminReportDetailResult.ReasonCount> aggregateReasonCounts(
            List<ReportJpaEntity> reports
    ) {
        Map<ReportType, Integer> counts = new EnumMap<>(ReportType.class);
        for (ReportJpaEntity report : reports) {
            if (report.getReportTypes() == null || report.getReportTypes().isBlank()) {
                continue;
            }
            for (String value : report.getReportTypes().split(",")) {
                ReportType type;
                try {
                    type = ReportType.valueOf(value.trim());
                } catch (IllegalArgumentException ignored) {
                    type = ReportType.OTHER;
                }
                counts.merge(type, 1, Integer::sum);
            }
        }

        List<AdminReportDetailResult.ReasonCount> result = new ArrayList<>();
        counts.forEach((reason, count) ->
                result.add(new AdminReportDetailResult.ReasonCount(reason, count)));
        result.sort(Comparator
                .comparingInt(AdminReportDetailResult.ReasonCount::count)
                .reversed()
                .thenComparingInt(reasonCount -> reasonCount.reason().ordinal()));
        return result;
    }

    private DetailTargetContent findDetailTargetContent(
            TargetType targetType,
            Long targetId,
            Long reportedMemberId
    ) {
        return switch (targetType) {
            case POST -> {
                PostJpaEntity post = entityManager.find(PostJpaEntity.class, targetId);
                if (post == null) {
                    yield new DetailTargetContent(
                            DELETED_POST_TITLE,
                            DETAIL_DELETED_POST_CONTENT,
                            null,
                            reportedMemberId
                    );
                }
                yield new DetailTargetContent(
                        post.getTitle(),
                        postDetailContent(post),
                        "/community/" + post.getId(),
                        post.getAuthorId()
                );
            }
            case COMMENT -> {
                CommentJpaEntity comment = entityManager.find(CommentJpaEntity.class, targetId);
                if (comment == null) {
                    yield new DetailTargetContent(
                            COMMENT_TITLE,
                            DELETED_COMMENT_CONTENT,
                            null,
                            reportedMemberId
                    );
                }
                yield new DetailTargetContent(
                        COMMENT_TITLE,
                        comment.getStatus() == CommentStatus.ACTIVE
                                ? comment.getContent()
                                : DELETED_COMMENT_CONTENT,
                        "/community/" + comment.getPostId(),
                        comment.getAuthorId()
                );
            }
            case REVIEW -> {
                ReviewJpaEntity review = entityManager.find(ReviewJpaEntity.class, targetId);
                if (review == null) {
                    yield new DetailTargetContent(
                            REVIEW_TITLE,
                            DELETED_REVIEW_CONTENT,
                            null,
                            reportedMemberId
                    );
                }
                yield new DetailTargetContent(
                        REVIEW_TITLE,
                        review.getStatus() == ReviewStatus.ACTIVE
                                ? review.getContent()
                                : DELETED_REVIEW_CONTENT,
                        "/courses/" + review.getCourseId(),
                        review.getMemberId()
                );
            }
        };
    }

    private MemberInfo findMemberInfo(Long memberId) {
        if (memberId == null) {
            return MemberInfo.EMPTY;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select name, username from members where member_id = :memberId")
                .setParameter("memberId", memberId)
                .setMaxResults(1)
                .getResultList();

        if (rows.isEmpty()) {
            return MemberInfo.EMPTY;
        }
        Object[] row = rows.get(0);
        return new MemberInfo(
                row[0] == null ? null : String.valueOf(row[0]),
                row[1] == null ? null : String.valueOf(row[1])
        );
    }

    private List<ReportTargetGroup> findTargetGroupPage(AdminReportListQuery query) {
        return createTargetGroupQuery(query)
                .setFirstResult(query.page() * query.size())
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(this::toReportTargetGroup)
                .toList();
    }

    private int countTargetGroups(AdminReportListQuery query) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                from reports latest
                where not exists (
                    select 1
                    from reports newer
                    where newer.target_type = latest.target_type
                      and newer.target_id = latest.target_id
                      and (
                          newer.created_at > latest.created_at
                          or (newer.created_at = latest.created_at and newer.report_id > latest.report_id)
                      )
                )
                  and (
                      select count(*)
                      from reports report
                      where report.target_type = latest.target_type
                        and report.target_id = latest.target_id
                  ) >= :minReportCount
                """);
        if (query.targetType() != null) {
            sql.append(" and latest.target_type = :targetType");
        }
        if (query.status() != null) {
            if (query.status() == ReportStatus.PENDING) {
                sql.append(" and (latest.status = :status or latest.status is null)");
            } else {
                sql.append(" and latest.status = :status");
            }
        }

        Query countQuery = entityManager.createNativeQuery(sql.toString())
                .setParameter("minReportCount", MIN_REPORT_COUNT_FOR_ADMIN_LIST);
        if (query.targetType() != null) {
            countQuery.setParameter("targetType", query.targetType().name());
        }
        if (query.status() != null) {
            countQuery.setParameter("status", query.status().name());
        }
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    private TypedQuery<Object[]> createTargetGroupQuery(AdminReportListQuery query) {
        return entityManager.createQuery("""
                select latest.id,
                       latest.targetType,
                       latest.targetId,
                       latest.reportedMemberId,
                       latest.status,
                       latest.reportTypes,
                       latest.createdAt,
                       count(report.id)
                from ReportJpaEntity latest, ReportJpaEntity report
                where report.targetType = latest.targetType
                  and report.targetId = latest.targetId
                  and (:targetType is null or latest.targetType = :targetType)
                  and (
                      :status is null
                      or latest.status = :status
                      or (:status = com.wanted.backend.domain.community.domain.model.ReportStatus.PENDING and latest.status is null)
                  )
                  and not exists (
                      select newer.id
                      from ReportJpaEntity newer
                      where newer.targetType = latest.targetType
                        and newer.targetId = latest.targetId
                        and (
                            newer.createdAt > latest.createdAt
                            or (newer.createdAt = latest.createdAt and newer.id > latest.id)
                        )
                  )
                group by latest.id,
                         latest.targetType,
                         latest.targetId,
                         latest.reportedMemberId,
                         latest.status,
                         latest.reportTypes,
                         latest.createdAt
                having count(report.id) >= :minReportCount
                order by latest.createdAt desc, latest.id desc
                """, Object[].class)
                .setParameter("targetType", query.targetType())
                .setParameter("status", query.status())
                .setParameter("minReportCount", (long) MIN_REPORT_COUNT_FOR_ADMIN_LIST);
    }

    private ReportTargetGroup toReportTargetGroup(Object[] row) {
        return new ReportTargetGroup(
                (Long) row[0],
                (TargetType) row[1],
                (Long) row[2],
                (Long) row[3],
                ((Long) row[7]).intValue(),
                normalizeStatus((ReportStatus) row[4]),
                (String) row[5],
                (LocalDateTime) row[6]
        );
    }

    private AdminReportListResult.Item toItem(ReportTargetGroup group) {
        TargetContent targetContent = findTargetContent(group.targetType(), group.targetId());

        return new AdminReportListResult.Item(
                group.reportId(),
                group.targetType(),
                group.targetId(),
                targetContent.title(),
                preview(targetContent.content()),
                representativeReason(group.reportTypes()),
                group.targetAuthorId(),
                findMemberName(group.targetAuthorId()),
                group.reportCount(),
                group.status(),
                group.reportedAt()
        );
    }

    private TargetContent findTargetContent(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> {
                PostJpaEntity post = entityManager.find(PostJpaEntity.class, targetId);
                yield post == null || post.getStatus() != PostStatus.ACTIVE
                        ? new TargetContent(DELETED_POST_TITLE, DELETED_POST_CONTENT)
                        : new TargetContent(post.getTitle(), post.getContent());
            }
            case COMMENT -> {
                CommentJpaEntity comment = entityManager.find(CommentJpaEntity.class, targetId);
                yield comment == null || comment.getStatus() != CommentStatus.ACTIVE
                        ? new TargetContent(COMMENT_TITLE, deletedCommentContent(comment))
                        : new TargetContent(COMMENT_TITLE, comment.getContent());
            }
            case REVIEW -> {
                ReviewJpaEntity review = entityManager.find(ReviewJpaEntity.class, targetId);
                yield review == null || review.getStatus() != ReviewStatus.ACTIVE
                        ? new TargetContent(REVIEW_TITLE, DELETED_REVIEW_CONTENT)
                        : new TargetContent(REVIEW_TITLE, review.getContent());
            }
        };
    }

    private String deletedCommentContent(CommentJpaEntity comment) {
        if (comment != null && comment.getStatus() == CommentStatus.ADMIN_DELETED) {
            return Comment.ADMIN_DELETED_MESSAGE;
        }
        return DELETED_COMMENT_CONTENT;
    }

    private String postDetailContent(PostJpaEntity post) {
        if (post.getStatus() == PostStatus.ACTIVE) {
            return post.getContent();
        }

        if (post.getStatus() == PostStatus.ADMIN_DELETED) {
            return DETAIL_DELETED_POST_CONTENT;
        }

        return DELETED_POST_CONTENT;
    }

    private String findMemberName(Long memberId) {
        if (memberId == null) {
            return null;
        }

        List<?> names = entityManager.createNativeQuery(
                        "select name from members where member_id = :memberId")
                .setParameter("memberId", memberId)
                .setMaxResults(1)
                .getResultList();

        return names.isEmpty() ? null : String.valueOf(names.get(0));
    }

    private ReportStatus normalizeStatus(ReportStatus status) {
        return status == null ? ReportStatus.PENDING : status;
    }

    private String representativeReason(String reportTypes) {
        if (reportTypes == null || reportTypes.isBlank()) {
            return null;
        }
        return reportTypes.split(",")[0].trim();
    }

    private String preview(String content) {
        if (content == null || content.length() <= PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, PREVIEW_LENGTH) + "...";
    }

    private int calculateTotalPages(int totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private record ReportTargetGroup(
            Long reportId,
            TargetType targetType,
            Long targetId,
            Long targetAuthorId,
            int reportCount,
            ReportStatus status,
            String reportTypes,
            LocalDateTime reportedAt
    ) {
    }

    private record TargetContent(
            String title,
            String content
    ) {
    }

    private record DetailTargetContent(
            String title,
            String content,
            String url,
            Long authorId
    ) {
    }

    private record MemberInfo(
            String name,
            String username
    ) {
        private static final MemberInfo EMPTY = new MemberInfo(null, null);
    }
}
