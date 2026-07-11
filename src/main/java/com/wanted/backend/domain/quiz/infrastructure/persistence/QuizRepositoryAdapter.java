package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuizRepositoryAdapter implements QuizRepository {

    private final QuizJpaRepository quizJpaRepository;

    @Override
    @Transactional
    public Quiz save(Quiz quiz) {
        QuizJpaEntity entity = QuizJpaEntity.create(
                quiz.getCourseId(), quiz.getSectionId(), quiz.getInstructorId(),
                quiz.getTitle(), quiz.getCreatedAt());

        appendQuestions(entity, quiz);

        QuizJpaEntity saved = quizJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> findById(Long id) {
        return quizJpaRepository.findWithQuestionsByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> findByIdIncludingDeleted(Long id) {
        return quizJpaRepository.findWithQuestionsById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> findAllByInstructor(Long instructorId, Long courseId, Long sectionId) {
        List<QuizJpaEntity> entities;
        if (courseId != null && sectionId != null) {
            entities = quizJpaRepository.findByInstructorIdAndCourseIdAndSectionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                    instructorId, courseId, sectionId);
        } else if (courseId != null) {
            entities = quizJpaRepository.findByInstructorIdAndCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                    instructorId, courseId);
        } else if (sectionId != null) {
            entities = quizJpaRepository.findByInstructorIdAndSectionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                    instructorId, sectionId);
        } else {
            entities = quizJpaRepository.findByInstructorIdAndDeletedAtIsNullOrderByCreatedAtAsc(instructorId);
        }

        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> findAllByCourseId(Long courseId) {
        return quizJpaRepository.findByCourseIdAndDeletedAtIsNullOrderBySectionIdAscIdAsc(courseId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Quiz update(Quiz quiz) {
        QuizJpaEntity entity = quizJpaRepository.findWithQuestionsByIdAndDeletedAtIsNull(quiz.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        entity.update(quiz.getCourseId(), quiz.getSectionId(), quiz.getTitle());

        entity.clearQuestions();
        // uq_quiz_question_quiz_number 유니크 키 때문에 기존 문항 DELETE가
        // 새 문항 INSERT보다 먼저 DB에 반영되어야 한다 (Hibernate는 INSERT를 먼저 실행).
        quizJpaRepository.flush();

        appendQuestions(entity, quiz);
        return toDomain(quizJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // 강사 수동 삭제도 soft-delete로 통일 — hard-delete하면 학생 제출 이력이
        // FK ON DELETE CASCADE로 함께 사라진다. 이미 삭제된 퀴즈면 no-op(멱등).
        quizJpaRepository.findByIdAndDeletedAtIsNull(id)
                .ifPresent(quiz -> quiz.softDelete(LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void deleteBySectionIds(List<Long> sectionIds) {
        // hard-delete하면 quiz_submission이 FK ON DELETE CASCADE로 함께 사라지므로,
        // 활성 퀴즈를 로딩해 soft-delete(deleted_at)로 전환한다. 제출 이력은 그대로 보존된다.
        LocalDateTime now = LocalDateTime.now();
        quizJpaRepository.findBySectionIdInAndDeletedAtIsNull(sectionIds)
                .forEach(quiz -> quiz.softDelete(now));
    }

    private void appendQuestions(QuizJpaEntity entity, Quiz quiz) {
        for (QuizQuestion question : quiz.getQuestions()) {
            QuizQuestionJpaEntity questionEntity = entity.addQuestion(
                    question.getQuestionNumber(), question.getQuestionText(), question.getExplanation());
            for (QuizOption option : question.getOptions()) {
                questionEntity.addOption(option.getOptionNumber(), option.getOptionText(), option.isCorrect());
            }
        }
    }

    private Quiz toDomain(QuizJpaEntity entity) {
        var questions = entity.getQuestions().stream()
                .map(q -> QuizQuestion.restore(
                        q.getId(), q.getQuestionNumber(), q.getQuestionText(), q.getExplanation(),
                        q.getOptions().stream()
                                .map(o -> QuizOption.restore(o.getId(), o.getOptionNumber(), o.getOptionText(), o.isCorrect()))
                                .toList()))
                .toList();

        return Quiz.restore(entity.getId(), entity.getInstructorId(), entity.getCourseId(),
                entity.getSectionId(), entity.getTitle(), questions, entity.getCreatedAt());
    }
}
