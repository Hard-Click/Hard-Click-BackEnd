package com.wanted.backend.domain.quiz.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizSubmission {

    private Long id;
    private Long quizId;
    private Long memberId;
    private int score;
    private int totalQuestionCount;
    private int correctCount;
    private LocalDateTime submittedAt;
    private List<QuizSubmissionAnswer> answers;

    private QuizSubmission() {}

    /**
     * 제출된 답안을 퀴즈 정답과 대조해 자동 채점한 제출 기록을 생성한다.
     *
     * @param answersByQuestionId 문항 id → 학생이 선택한 보기 id (미응답 문항은 값이 없거나 null)
     */
    public static QuizSubmission grade(Long memberId, Quiz quiz, Map<Long, Long> answersByQuestionId) {
        return grade(memberId, quiz, answersByQuestionId, null);
    }

    /**
     * @param timeSpentByQuestionId 문항 id → 풀이 체류 시간(초, 재방문 누적). 미측정 문항은 값이 없거나 null
     */
    public static QuizSubmission grade(Long memberId, Quiz quiz, Map<Long, Long> answersByQuestionId,
                                       Map<Long, Integer> timeSpentByQuestionId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 식별자는 필수입니다.");
        }
        if (quiz == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("채점할 퀴즈 문항이 없습니다.");
        }

        List<QuizSubmissionAnswer> gradedAnswers = new ArrayList<>();
        int correct = 0;
        for (QuizQuestion question : quiz.getQuestions()) {
            Long correctOptionId = correctOptionId(question);
            Long selectedOptionId = answersByQuestionId == null ? null : answersByQuestionId.get(question.getId());
            Integer timeSpentSeconds = timeSpentByQuestionId == null ? null : timeSpentByQuestionId.get(question.getId());
            boolean isCorrect = selectedOptionId != null && selectedOptionId.equals(correctOptionId);
            if (isCorrect) {
                correct++;
            }
            gradedAnswers.add(QuizSubmissionAnswer.create(question.getId(), selectedOptionId, isCorrect, timeSpentSeconds));
        }

        int total = gradedAnswers.size();

        QuizSubmission submission = new QuizSubmission();
        submission.quizId = quiz.getId();
        submission.memberId = memberId;
        submission.totalQuestionCount = total;
        submission.correctCount = correct;
        submission.score = Math.round((float) correct * 100 / total);
        submission.submittedAt = LocalDateTime.now();
        submission.answers = gradedAnswers;
        return submission;
    }

    public static QuizSubmission restore(Long id, Long quizId, Long memberId, int score,
                                          int totalQuestionCount, int correctCount, LocalDateTime submittedAt,
                                          List<QuizSubmissionAnswer> answers) {
        QuizSubmission submission = new QuizSubmission();
        submission.id = id;
        submission.quizId = quizId;
        submission.memberId = memberId;
        submission.score = score;
        submission.totalQuestionCount = totalQuestionCount;
        submission.correctCount = correctCount;
        submission.submittedAt = submittedAt;
        submission.answers = new ArrayList<>(answers);
        return submission;
    }

    private static Long correctOptionId(QuizQuestion question) {
        return question.getOptions().stream()
                .filter(QuizOption::isCorrect)
                .map(QuizOption::getId)
                .findFirst()
                .orElse(null);
    }

    public int getIncorrectCount() {
        return totalQuestionCount - correctCount;
    }

    public Long getId() { return id; }
    public Long getQuizId() { return quizId; }
    public Long getMemberId() { return memberId; }
    public int getScore() { return score; }
    public int getTotalQuestionCount() { return totalQuestionCount; }
    public int getCorrectCount() { return correctCount; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public List<QuizSubmissionAnswer> getAnswers() { return List.copyOf(answers); }
}
