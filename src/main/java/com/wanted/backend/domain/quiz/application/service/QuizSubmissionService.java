package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitQuizCommand;
import com.wanted.backend.domain.quiz.application.result.QuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.usecase.QuizSubmissionUseCase;
import com.wanted.backend.domain.quiz.domain.event.QuizSubmittedEvent;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizSubmissionService implements QuizSubmissionUseCase {

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public QuizSubmissionResult submit(SubmitQuizCommand command) {
        Quiz quiz = quizRepository.findById(command.quizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 1인 1제출 정책 — 재제출 방지
        if (quizSubmissionRepository.existsByQuizIdAndMemberId(command.quizId(), command.memberId())) {
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        Map<Long, Long> answersByQuestionId = new HashMap<>();
        if (command.answers() != null) {
            command.answers().forEach(a -> answersByQuestionId.put(a.questionId(), a.selectedOptionId()));
        }

        QuizSubmission submission = QuizSubmission.grade(command.memberId(), quiz, answersByQuestionId);

        QuizSubmission saved;
        try {
            saved = quizSubmissionRepository.save(submission);
        } catch (DataIntegrityViolationException e) {
            // exists 사전 체크와 save 사이 경쟁(TOCTOU) — UNIQUE 제약이 중복을 막았다.
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        // 채점 결과를 AI(Python) 서버로 비동기 전송 (트랜잭션 커밋 후 리스너에서 처리)
        eventPublisher.publishEvent(QuizSubmittedEvent.of(
                saved.getId(), saved.getQuizId(), saved.getMemberId(), saved.getScore(),
                saved.getTotalQuestionCount(), saved.getCorrectCount(),
                saved.getAnswers().stream()
                        .map(a -> new QuizSubmittedEvent.AnsweredQuestion(
                                a.getQuestionId(), a.getSelectedOptionId(), a.isCorrect()))
                        .toList()));

        return new QuizSubmissionResult(
                saved.getId(), saved.getQuizId(), saved.getScore(), saved.getTotalQuestionCount(),
                saved.getCorrectCount(), saved.getIncorrectCount(), saved.getSubmittedAt());
    }
}
