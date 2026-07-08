package com.wanted.backend.domain.quiz.application.listener;

import com.wanted.backend.domain.quiz.application.port.QuizSubmissionAiPort;
import com.wanted.backend.domain.quiz.domain.event.QuizSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 퀴즈 제출 이벤트를 받아 채점 내용을 AI(Python) 서버로 비동기 전송한다.
 * 트랜잭션 커밋 이후(AFTER_COMMIT)에만 실행되므로, AI 전송 실패가 제출/채점 저장을 되돌리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSubmissionAiListener {

    private final QuizSubmissionAiPort quizSubmissionAiPort;

    @Async("quizAiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuizSubmitted(QuizSubmittedEvent event) {
        try {
            quizSubmissionAiPort.sendSubmission(new QuizSubmissionAiPort.SubmissionPayload(
                    event.submissionId(),
                    event.quizId(),
                    event.memberId(),
                    event.score(),
                    event.totalQuestionCount(),
                    event.correctCount(),
                    event.answers().stream()
                            .map(a -> new QuizSubmissionAiPort.AnswerPayload(
                                    a.questionId(), a.selectedOptionId(), a.correct()))
                            .toList()));
        } catch (Exception e) {
            // AI 전송 실패는 학생 제출 결과에 영향을 주지 않는다 — 로깅만 하고 삼킨다.
            log.error("퀴즈 제출 AI 전송 실패 (submissionId={}): {}", event.submissionId(), e.getMessage(), e);
        }
    }
}
