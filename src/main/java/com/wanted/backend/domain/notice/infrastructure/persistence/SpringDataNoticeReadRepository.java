package com.wanted.backend.domain.notice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataNoticeReadRepository extends JpaRepository<NoticeReadJpaEntity, Long> {

    boolean existsByMemberIdAndNoticeId(Long memberId, Long noticeId);

    // QUERY_002 규칙: JPQL/native 애노테이션 대신 파생 쿼리 사용. 어댑터에서 noticeId만 추출한다.
    List<NoticeReadJpaEntity> findByMemberIdAndNoticeIdIn(Long memberId, List<Long> noticeIds);
}
