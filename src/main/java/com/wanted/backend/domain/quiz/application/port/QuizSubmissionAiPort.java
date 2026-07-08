package com.wanted.backend.domain.quiz.application.port;

import java.util.List;

/**
 * 채점된 퀴즈 제출 내용을 AI(Python) 서버로 전달하는 아웃바운드 포트.
 * 실제 전송은 인프라 어댑터가 담당하며, 호출은 트랜잭션 커밋 후 비동기로 이루어진다.
 */
public interface QuizSubmissionAiPort {

    void sendSubmission(SubmissionPayload payload);

    record SubmissionPayload(
            Long submissionId,
            Long quizId,
            Long memberId,
            int score,
            int totalQuestionCount,
            int correctCount,
            List<AnswerPayload> answers
    ) {}

    record AnswerPayload(Long questionId, Long selectedOptionId, boolean correct) {}
}
