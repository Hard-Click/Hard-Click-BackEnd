package com.wanted.backend.domain.quiz.domain.model;

public class QuizSubmissionAnswer {

    private Long id;
    private Long questionId;
    private Long selectedOptionId;
    private boolean correct;

    private QuizSubmissionAnswer() {}

    static QuizSubmissionAnswer create(Long questionId, Long selectedOptionId, boolean correct) {
        if (questionId == null) {
            throw new IllegalArgumentException("문항 식별자는 필수입니다.");
        }
        QuizSubmissionAnswer answer = new QuizSubmissionAnswer();
        answer.questionId = questionId;
        answer.selectedOptionId = selectedOptionId;
        answer.correct = correct;
        return answer;
    }

    public static QuizSubmissionAnswer restore(Long id, Long questionId, Long selectedOptionId, boolean correct) {
        QuizSubmissionAnswer answer = new QuizSubmissionAnswer();
        answer.id = id;
        answer.questionId = questionId;
        answer.selectedOptionId = selectedOptionId;
        answer.correct = correct;
        return answer;
    }

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public Long getSelectedOptionId() { return selectedOptionId; }
    public boolean isCorrect() { return correct; }
}
