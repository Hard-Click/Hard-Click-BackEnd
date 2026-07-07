package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        for (QuizQuestion question : quiz.getQuestions()) {
            QuizQuestionJpaEntity questionEntity = entity.addQuestion(
                    question.getQuestionNumber(), question.getQuestionText(), question.getExplanation());
            for (QuizOption option : question.getOptions()) {
                questionEntity.addOption(option.getOptionNumber(), option.getOptionText(), option.isCorrect());
            }
        }

        QuizJpaEntity saved = quizJpaRepository.save(entity);
        return toDomain(saved);
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
