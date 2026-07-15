package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudentExamScoreJpaRepository extends JpaRepository<StudentExamScoreJpaEntity, Long> {

    List<StudentExamScoreJpaEntity> findByMemberId(Long memberId);

    /**
     * 같은 응시일 성적을 지운다 - 온보딩 화면을 다시 저장했을 때 덮어쓰기 위한 선삭제.
     *
     * <p>파생 쿼리(deleteBy...)를 쓰면 안 된다. 그건 SELECT 후 EntityManager.remove() 로 삭제를
     * 영속성 컨텍스트에 쌓아두는데, Hibernate 의 flush 순서는 <b>INSERT 가 DELETE 보다 먼저</b>다.
     * 그래서 재저장 시 새 INSERT 가 아직 안 지워진 기존 행과 부딪혀
     * uq_exam_member_area_date 에 Duplicate entry 로 터진다.
     * 벌크 DELETE 는 즉시 DML 로 나가므로 순서 문제가 없다.
     * flushAutomatically/clearAutomatically 로 영속성 컨텍스트와의 불일치도 막는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StudentExamScoreJpaEntity s WHERE s.memberId = :memberId AND s.examDate = :examDate")
    void deleteByMemberIdAndExamDate(@Param("memberId") Long memberId, @Param("examDate") LocalDate examDate);

    boolean existsByMemberId(Long memberId);
}
