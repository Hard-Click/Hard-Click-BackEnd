package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmissionAnswer;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QuizSubmissionRepositoryAdapter implements QuizSubmissionRepository {

    private final QuizSubmissionJpaRepository quizSubmissionJpaRepository;

    @Override
    @Transactional
    public QuizSubmission save(QuizSubmission submission) {
        QuizSubmissionJpaEntity entity = QuizSubmissionJpaEntity.create(
                submission.getQuizId(), submission.getMemberId(), submission.getScore(),
                submission.getTotalQuestionCount(), submission.getCorrectCount(), submission.getSubmittedAt());

        for (QuizSubmissionAnswer answer : submission.getAnswers()) {
            entity.addAnswer(answer.getQuestionId(), answer.getSelectedOptionId(), answer.isCorrect());
        }

        return toDomain(quizSubmissionJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByQuizIdAndMemberId(Long quizId, Long memberId) {
        return quizSubmissionJpaRepository.existsByQuizIdAndMemberId(quizId, memberId);
    }

    private QuizSubmission toDomain(QuizSubmissionJpaEntity entity) {
        var answers = entity.getAnswers().stream()
                .map(a -> QuizSubmissionAnswer.restore(
                        a.getId(), a.getQuestionId(), a.getSelectedOptionId(), a.isCorrect()))
                .toList();

        return QuizSubmission.restore(entity.getId(), entity.getQuizId(), entity.getMemberId(),
                entity.getScore(), entity.getTotalQuestionCount(), entity.getCorrectCount(),
                entity.getSubmittedAt(), answers);
    }
}
