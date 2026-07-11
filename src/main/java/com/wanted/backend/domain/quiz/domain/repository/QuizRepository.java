package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.Quiz;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {

    Quiz save(Quiz quiz);

    List<Quiz> findAllByInstructor(Long instructorId, Long courseId, Long sectionId);

    List<Quiz> findAllByCourseId(Long courseId);

    Optional<Quiz> findById(Long id);

    Quiz update(Quiz quiz);

    void deleteById(Long id);

    // 섹션 삭제 cascade: 해당 섹션들에 속한 퀴즈를 일괄 삭제한다(문항·보기·제출은 FK ON DELETE CASCADE).
    void deleteBySectionIds(List<Long> sectionIds);
}
