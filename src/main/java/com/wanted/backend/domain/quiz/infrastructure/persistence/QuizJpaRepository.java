package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizJpaRepository extends JpaRepository<QuizJpaEntity, Long> {

    // 단일 퀴즈 조회는 questions까지 한 번에 로딩한다 (N+1 결정 순서: 단일→fetch join).
    // 쿼리문 직접 삽입 금지 규칙에 따라 JPQL 대신 @EntityGraph + 메서드 네이밍으로 표현.
    // 문항별 options까지 중첩 fetch하면 MultipleBagFetchException이 나므로
    // options는 컬렉션 @BatchSize로 IN 조회한다 (다중→batch size).
    //
    // 활성(soft-delete 안 된) 퀴즈만 — 응시/수정/상세 등 활성 경로에서 사용.
    @EntityGraph(attributePaths = "questions")
    Optional<QuizJpaEntity> findWithQuestionsByIdAndDeletedAtIsNull(Long id);

    // 삭제 여부와 무관하게 조회 — 학생 과거 리포트는 soft-delete된 퀴즈도 렌더해야 한다.
    @EntityGraph(attributePaths = "questions")
    Optional<QuizJpaEntity> findWithQuestionsById(Long id);

    // 활성 퀴즈 단건(questions 불필요한 soft-delete 처리용) — 수동 삭제 시 사용.
    Optional<QuizJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    // 목록 조회는 다중 건이라 fetch join 대신 questions/options 컬렉션의 @BatchSize로 로딩한다.
    // 모든 목록은 활성(deleted_at IS NULL) 퀴즈만 노출한다.
    List<QuizJpaEntity> findByInstructorIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long instructorId);

    List<QuizJpaEntity> findByInstructorIdAndCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long instructorId, Long courseId);

    List<QuizJpaEntity> findByInstructorIdAndSectionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long instructorId, Long sectionId);

    List<QuizJpaEntity> findByInstructorIdAndCourseIdAndSectionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long instructorId, Long courseId, Long sectionId);

    // 학생 '내 퀴즈 목록' — 특정 강의의 활성 퀴즈 전체. questions를 함께 로딩(문항 수 계산)하고,
    // 주차 정렬은 앱에서 섹션 order_index로 하되 동일 주차 내 순서 결정성을 위해 sectionId,id로 1차 정렬한다.
    @EntityGraph(attributePaths = "questions")
    List<QuizJpaEntity> findByCourseIdAndDeletedAtIsNullOrderBySectionIdAscIdAsc(Long courseId);

    // 섹션 삭제 cascade용 — 해당 섹션들의 활성 퀴즈를 로딩해 soft-delete 대상으로 삼는다
    // (hard-delete하지 않으므로 quiz_submission FK 이력이 보존된다).
    List<QuizJpaEntity> findBySectionIdInAndDeletedAtIsNull(Collection<Long> sectionIds);
}
