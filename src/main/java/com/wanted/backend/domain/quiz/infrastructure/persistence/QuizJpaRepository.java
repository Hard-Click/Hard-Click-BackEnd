package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizJpaRepository extends JpaRepository<QuizJpaEntity, Long> {

    // 단일 퀴즈 조회는 questions까지 한 번에 로딩한다 (N+1 결정 순서: 단일→fetch join).
    // 쿼리문 직접 삽입 금지 규칙에 따라 JPQL 대신 @EntityGraph + 메서드 네이밍으로 표현.
    // 문항별 options까지 중첩 fetch하면 MultipleBagFetchException이 나므로
    // options는 컬렉션 @BatchSize로 IN 조회한다 (다중→batch size).
    @EntityGraph(attributePaths = "questions")
    Optional<QuizJpaEntity> findWithQuestionsById(Long id);

    // 목록 조회는 다중 건이라 fetch join 대신 questions/options 컬렉션의 @BatchSize로 로딩한다.
    List<QuizJpaEntity> findByInstructorIdOrderByCreatedAtAsc(Long instructorId);

    List<QuizJpaEntity> findByInstructorIdAndCourseIdOrderByCreatedAtAsc(Long instructorId, Long courseId);

    List<QuizJpaEntity> findByInstructorIdAndSectionIdOrderByCreatedAtAsc(Long instructorId, Long sectionId);

    List<QuizJpaEntity> findByInstructorIdAndCourseIdAndSectionIdOrderByCreatedAtAsc(
            Long instructorId, Long courseId, Long sectionId);

    // 학생 '내 퀴즈 목록' — 특정 강의의 전체 퀴즈. questions를 함께 로딩(문항 수 계산)하고,
    // 주차 정렬은 앱에서 섹션 order_index로 하되 동일 주차 내 순서 결정성을 위해 sectionId,id로 1차 정렬한다.
    @EntityGraph(attributePaths = "questions")
    List<QuizJpaEntity> findByCourseIdOrderBySectionIdAscIdAsc(Long courseId);
}
