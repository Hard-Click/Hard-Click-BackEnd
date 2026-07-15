package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentAvailabilityJpaRepository extends JpaRepository<StudentAvailabilityJpaEntity, Long> {

    List<StudentAvailabilityJpaEntity> findByMemberIdOrderByDayOfWeekAscStartTimeAsc(Long memberId);

    /**
     * 가용시간은 전체 덮어쓰기다 - 저장 전 기존 구간을 지운다.
     *
     * <p>student_exam_score 와 같은 이유로 벌크 DELETE 를 쓴다(파생 deleteBy 는 INSERT 가 먼저
     * flush 돼 기존 행과 공존한다). 이 테이블엔 UNIQUE 제약이 없어 지금은 터지지 않지만,
     * 삭제 전 INSERT 가 나가는 건 동일하므로 같은 방식으로 맞춘다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StudentAvailabilityJpaEntity a WHERE a.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
