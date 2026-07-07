package com.wanted.backend.domain.quiz.domain.model;

public class QuizOption {

    private Long id;
    private int optionNumber;
    private String optionText;
    private boolean correct;

    private QuizOption() {}

    static QuizOption create(int optionNumber, String optionText, boolean correct) {
        if (optionText == null || optionText.isBlank()) {
            throw new IllegalArgumentException("보기 내용은 필수입니다.");
        }
        QuizOption option = new QuizOption();
        option.optionNumber = optionNumber;
        option.optionText = optionText;
        option.correct = correct;
        return option;
    }

    public static QuizOption restore(Long id, int optionNumber, String optionText, boolean correct) {
        QuizOption option = new QuizOption();
        option.id = id;
        option.optionNumber = optionNumber;
        option.optionText = optionText;
        option.correct = correct;
        return option;
    }

    public Long getId() { return id; }
    public int getOptionNumber() { return optionNumber; }
    public String getOptionText() { return optionText; }
    public boolean isCorrect() { return correct; }
}
