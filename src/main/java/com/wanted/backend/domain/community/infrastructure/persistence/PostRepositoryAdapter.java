package com.wanted.backend.domain.community.infrastructure.persistence;

import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import com.wanted.backend.domain.community.domain.model.PostStatus;
import com.wanted.backend.domain.community.domain.model.PostSummary;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public class PostRepositoryAdapter implements PostRepository {

    private final SpringDataPostRepository repository;

    public PostRepositoryAdapter(SpringDataPostRepository repository) {
        this.repository = repository;
    }

    @PersistenceContext
    private EntityManager em;

    @Override
    public Post save(Post post) {
        if (post.getId() == null) {
            PostJpaEntity entity = new PostJpaEntity(
                    post.getAuthorId(), post.getBoardType(), post.getSubject(),
                    post.getTitle(), post.getContent(), post.getViewCount(),
                    post.isAccepted(), post.getCreatedAt(), post.getUpdatedAt()
            );
            return toDomain(repository.save(entity));
        }

        PostJpaEntity entity = repository.findById(post.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        entity.update(
                post.getSubject(), post.getTitle(), post.getContent(),
                post.getViewCount(), post.getStatus(), post.isAccepted(), post.getUpdatedAt()
        );
        return toDomain(repository.save(entity));
    }

    @Override
    public List<Post> findByBoardType(BoardType boardType, PostSortType sort,
                                      String keyword, int page, int size) {
        if (sort == PostSortType.comments) {
            return em.createQuery("""
                SELECT p FROM PostJpaEntity p
                WHERE p.boardType = :boardType
                  AND p.title LIKE :keyword
                  AND p.status = :status
                ORDER BY (
                    SELECT COUNT(c) FROM CommentJpaEntity c WHERE c.postId = p.id
                ) DESC
                """, PostJpaEntity.class)
                    .setParameter("boardType", boardType)
                    .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                    .setParameter("status", PostStatus.ACTIVE)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList()
                    .stream().map(this::toDomain).toList();
        }
        Pageable pageable = PageRequest.of(page, size, toSort(sort));
        return repository.findByBoardTypeAndTitleContainingAndStatus(
                        boardType, keyword != null ? keyword : "", PostStatus.ACTIVE, pageable)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Post> findAll(PostSortType sort, String keyword, int page, int size) {
        if (sort == PostSortType.comments) {
            return em.createQuery("""
                SELECT p FROM PostJpaEntity p
                WHERE p.title LIKE :keyword
                  AND p.status = :status
                ORDER BY (
                    SELECT COUNT(c) FROM CommentJpaEntity c WHERE c.postId = p.id
                ) DESC
                """, PostJpaEntity.class)
                    .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                    .setParameter("status", PostStatus.ACTIVE)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList()
                    .stream().map(this::toDomain).toList();
        }
        Pageable pageable = PageRequest.of(page, size, toSort(sort));
        return repository.findByTitleContainingAndStatus(
                        keyword != null ? keyword : "", PostStatus.ACTIVE, pageable)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // 방법③: JOIN + DTO Projection — 상관 서브쿼리 대신 LEFT JOIN + GROUP BY로 작성자명/댓글수까지
    // 한 쿼리에서 채워 반환한다. 호출부(PostQueryService)가 별도 Batch IN을 또 돌릴 필요가 없다.
    @Override
    public List<PostSummary> findSummaryByBoardTypeOrderByCommentCount(BoardType boardType, String keyword, int page, int size) {
        return em.createQuery("""
                SELECT new com.wanted.backend.domain.community.domain.model.PostSummary(
                    p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount, COUNT(c.id))
                FROM PostJpaEntity p
                JOIN MemberReferenceEntity m ON m.id = p.authorId
                LEFT JOIN CommentJpaEntity c ON c.postId = p.id
                WHERE p.boardType = :boardType AND p.title LIKE :keyword AND p.status = :status
                GROUP BY p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount
                ORDER BY COUNT(c.id) DESC
                """, PostSummary.class)
                .setParameter("boardType", boardType)
                .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                .setParameter("status", PostStatus.ACTIVE)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public List<PostSummary> findAllSummaryOrderByCommentCount(String keyword, int page, int size) {
        return em.createQuery("""
                SELECT new com.wanted.backend.domain.community.domain.model.PostSummary(
                    p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount, COUNT(c.id))
                FROM PostJpaEntity p
                JOIN MemberReferenceEntity m ON m.id = p.authorId
                LEFT JOIN CommentJpaEntity c ON c.postId = p.id
                WHERE p.title LIKE :keyword AND p.status = :status
                GROUP BY p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount
                ORDER BY COUNT(c.id) DESC
                """, PostSummary.class)
                .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                .setParameter("status", PostStatus.ACTIVE)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    // 방법④: 비정규화 — comment_count 컬럼을 그대로 읽기만 함. COUNT 서브쿼리도, CommentJpaEntity JOIN도 없음.
    @Override
    public List<PostSummary> findSummaryByBoardTypeOrderByCommentCountDenormalized(BoardType boardType, String keyword, int page, int size) {
        return em.createQuery("""
                SELECT new com.wanted.backend.domain.community.domain.model.PostSummary(
                    p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount, p.commentCount)
                FROM PostJpaEntity p
                JOIN MemberReferenceEntity m ON m.id = p.authorId
                WHERE p.boardType = :boardType AND p.title LIKE :keyword AND p.status = :status
                ORDER BY p.commentCount DESC
                """, PostSummary.class)
                .setParameter("boardType", boardType)
                .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                .setParameter("status", PostStatus.ACTIVE)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public List<PostSummary> findAllSummaryOrderByCommentCountDenormalized(String keyword, int page, int size) {
        return em.createQuery("""
                SELECT new com.wanted.backend.domain.community.domain.model.PostSummary(
                    p.id, p.boardType, p.subject, p.title, m.name, p.createdAt, p.viewCount, p.commentCount)
                FROM PostJpaEntity p
                JOIN MemberReferenceEntity m ON m.id = p.authorId
                WHERE p.title LIKE :keyword AND p.status = :status
                ORDER BY p.commentCount DESC
                """, PostSummary.class)
                .setParameter("keyword", "%" + (keyword != null ? keyword : "") + "%")
                .setParameter("status", PostStatus.ACTIVE)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public void incrementCommentCount(Long postId) {
        repository.incrementCommentCount(postId);
    }

    @Override
    public void decrementCommentCount(Long postId) {
        repository.decrementCommentCount(postId);
    }

    @Override
    public int countByBoardType(BoardType boardType, String keyword) {
        return repository.countByBoardTypeAndTitleContainingAndStatus(
                boardType, keyword != null ? keyword : "", PostStatus.ACTIVE);
    }

    @Override
    public int countAll(String keyword) {
        return repository.countByTitleContainingAndStatus(
                keyword != null ? keyword : "", PostStatus.ACTIVE);
    }

    private Sort toSort(PostSortType sort) {
        return switch (sort) {
            case views -> Sort.by(Sort.Direction.DESC, "viewCount");
            case comments -> Sort.by(Sort.Direction.DESC, "createdAt");
            case latest -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Post toDomain(PostJpaEntity entity) {
        return Post.restore(
                entity.getId(), entity.getAuthorId(), entity.getBoardType(),
                entity.getSubject(),
                entity.getTitle(), entity.getContent(),
                entity.getViewCount(), entity.getStatus(), entity.isAccepted(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }


    @Override
    public Optional<Post> findById(Long postId) {
        return repository.findById(postId).map(this::toDomain);
    }

    @Override
    public void updateViewCount(Long postId, int viewCount) {
        repository.findById(postId).ifPresent(entity -> {
            entity.updateViewCount(viewCount);
            repository.save(entity);
        });
    }

    @Override
    public void deleteById(Long postId) {
        repository.deleteById(postId);
    }

    @Override
    public void adminDeleteById(Long postId) {
        PostJpaEntity entity = repository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Post post = toDomain(entity);
        post.softDeleteByAdmin(LocalDateTime.now());

        entity.update(
                post.getSubject(), post.getTitle(), post.getContent(),
                post.getViewCount(), post.getStatus(), post.isAccepted(), post.getUpdatedAt()
        );
        repository.save(entity);
    }

}
