package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {

    Quiz save(Quiz quiz);

    List<Quiz> findAllByInstructor(Long instructorId, Long courseId, Long sectionId);

    List<Quiz> findAllByCourseId(Long courseId);

    // 문항 id 목록으로 직접 조회 — 유사퀴즈 채점(②)에서 저장된 문항만 로딩한다(코스 전체 로딩 회피).
    List<QuizQuestion> findQuestionsByIds(List<Long> questionIds);

    // questionIds 중 실제로 courseId 소속인 것만 반환 — 복습 그룹 영속 전, 추천기가 준 courseId를
    // 신뢰하지 않고 DB 기준으로 문항 소속을 검증하기 위해 사용.
    List<Long> findQuestionIdsBelongingToCourse(List<Long> questionIds, Long courseId);

    // 활성(soft-delete 안 된) 퀴즈만 조회 — 응시/수정/상세 경로에서 사용.
    Optional<Quiz> findById(Long id);

    // 삭제 여부와 무관하게 조회 — 학생 과거 리포트는 soft-delete된 퀴즈도 렌더해야 하므로 사용.
    Optional<Quiz> findByIdIncludingDeleted(Long id);

    Quiz update(Quiz quiz);

    // 강사 수동 삭제 — soft-delete로 처리한다(학생 제출 이력 보존). 섹션 cascade와 동일 정책.
    void deleteById(Long id);

    // 섹션 삭제 cascade: 해당 섹션들의 퀴즈를 soft-delete 한다.
    // hard-delete하지 않으므로 학생 제출 이력(quiz_submission)이 보존된다.
    void deleteBySectionIds(List<Long> sectionIds);
}
