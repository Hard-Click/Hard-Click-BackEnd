package com.wanted.backend.domain.community.infrastructure.persistence;

import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.PostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataPostRepository extends JpaRepository<PostJpaEntity, Long> {

    // 게시글 수정 시 동시 PATCH의 첨부파일 개수 race 방지 — 게시글 행에 쓰기 잠금을 걸어
    // 같은 게시글에 대한 수정 요청을 직렬화한다(뒤 요청은 앞 요청 커밋 후 최신 상태를 읽음).
    // 파생 쿼리(find..By Id) + @Lock 조합으로 @Query 없이 SELECT ... FOR UPDATE를 건다.
    // ("WithLock"은 find와 By 사이의 무시되는 서술어 — 파생 파싱 대상은 Id 뿐이다.)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PostJpaEntity> findWithLockById(Long id);

    // boardType + 키워드 검색 + 페이징
    List<PostJpaEntity> findByBoardTypeAndTitleContainingAndStatus(
            BoardType boardType, String keyword, PostStatus status, Pageable pageable);

    // 전체 + 키워드 검색 + 페이징
    List<PostJpaEntity> findByTitleContainingAndStatus(
            String keyword, PostStatus status, Pageable pageable);

    // boardType + 키워드 검색 전체 수
    int countByBoardTypeAndTitleContainingAndStatus(
            BoardType boardType, String keyword, PostStatus status);

    // 전체 + 키워드 검색 전체 수
    int countByTitleContainingAndStatus(String keyword, PostStatus status);

    List<PostJpaEntity> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, PostStatus status);

    // 방법④: 댓글 생성/삭제 시 comment_count 동기화 — 원자적 UPDATE (조회 후 저장 방식의 레이스 방지)
    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(@Param("postId") Long postId);

}
