package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmissionAnswer;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            entity.addAnswer(answer.getQuestionId(), answer.getSelectedOptionId(), answer.isCorrect(),
                    answer.getTimeSpentSeconds());
        }

        // saveAndFlush로 즉시 INSERT를 실행해, UNIQUE(quiz_id, member_id) 위반이
        // 트랜잭션 커밋 시점이 아니라 이 호출 안에서 발생하도록 한다
        // (서비스가 DataIntegrityViolationException을 잡아 QZ003으로 변환할 수 있게).
        return toDomain(quizSubmissionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByQuizIdAndMemberId(Long quizId, Long memberId) {
        return quizSubmissionJpaRepository.existsByQuizIdAndMemberId(quizId, memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSubmission> findByMemberIdAndQuizIdIn(Long memberId, List<Long> quizIds) {
        if (quizIds.isEmpty()) {
            return List.of();
        }
        return quizSubmissionJpaRepository.findByMemberIdAndQuizIdIn(memberId, quizIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSubmission> findByQuizId(Long quizId) {
        return quizSubmissionJpaRepository.findByQuizId(quizId).stream()
                .map(this::toDomain)
                .toList();
    }

    private QuizSubmission toDomain(QuizSubmissionJpaEntity entity) {
        var answers = entity.getAnswers().stream()
                .map(a -> QuizSubmissionAnswer.restore(
                        a.getId(), a.getQuestionId(), a.getSelectedOptionId(), a.isCorrect(),
                        a.getTimeSpentSeconds()))
                .toList();

        return QuizSubmission.restore(entity.getId(), entity.getQuizId(), entity.getMemberId(),
                entity.getScore(), entity.getTotalQuestionCount(), entity.getCorrectCount(),
                entity.getSubmittedAt(), answers);
    }
}
