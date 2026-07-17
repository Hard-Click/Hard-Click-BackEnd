package com.wanted.backend.domain.quiz.domain.model;

public class QuizSubmissionAnswer {

    /** 문제당 체류 시간 상한(초). 범위 밖 값은 신뢰할 수 없으므로 미측정(null)으로 취급한다. */
    static final int MAX_TIME_SPENT_SECONDS = 3600;

    private Long id;
    private Long questionId;
    private Long selectedOptionId;
    private boolean correct;
    private Integer timeSpentSeconds;

    private QuizSubmissionAnswer() {}

    static QuizSubmissionAnswer create(Long questionId, Long selectedOptionId, boolean correct,
                                       Integer timeSpentSeconds) {
        if (questionId == null) {
            throw new IllegalArgumentException("문항 식별자는 필수입니다.");
        }
        QuizSubmissionAnswer answer = new QuizSubmissionAnswer();
        answer.questionId = questionId;
        answer.selectedOptionId = selectedOptionId;
        answer.correct = correct;
        answer.timeSpentSeconds = normalizeTimeSpent(timeSpentSeconds);
        return answer;
    }

    public static QuizSubmissionAnswer restore(Long id, Long questionId, Long selectedOptionId, boolean correct,
                                               Integer timeSpentSeconds) {
        QuizSubmissionAnswer answer = new QuizSubmissionAnswer();
        answer.id = id;
        answer.questionId = questionId;
        answer.selectedOptionId = selectedOptionId;
        answer.correct = correct;
        answer.timeSpentSeconds = timeSpentSeconds;
        return answer;
    }

    private static Integer normalizeTimeSpent(Integer seconds) {
        if (seconds == null || seconds < 0 || seconds > MAX_TIME_SPENT_SECONDS) {
            return null;
        }
        return seconds;
    }

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public Long getSelectedOptionId() { return selectedOptionId; }
    public boolean isCorrect() { return correct; }
    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
}
