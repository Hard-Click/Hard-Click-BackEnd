package com.wanted.backend.domain.quiz.domain.model;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {

    private static final int REQUIRED_OPTION_COUNT = 4;

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 3;

    private Long id;
    private int questionNumber;
    private String questionText;
    private String explanation;
    private Integer difficulty;   // 1=하/2=중/3=상, null=미지정
    private List<QuizOption> options;

    private QuizQuestion() {}

    public static QuizQuestion create(int questionNumber, String questionText, String explanation,
                                       Integer difficulty, int correctOptionNumber, List<String> optionTexts) {
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("문제 내용은 필수입니다.");
        }
        if (optionTexts == null || optionTexts.size() != REQUIRED_OPTION_COUNT) {
            throw new IllegalArgumentException("문항에는 " + REQUIRED_OPTION_COUNT + "개의 보기가 필요합니다.");
        }
        if (correctOptionNumber < 1 || correctOptionNumber > optionTexts.size()) {
            throw new IllegalArgumentException("정답 보기 번호가 올바르지 않습니다.");
        }
        if (difficulty != null && (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY)) {
            throw new IllegalArgumentException("난이도는 1(하)~3(상) 사이여야 합니다.");
        }

        QuizQuestion question = new QuizQuestion();
        question.questionNumber = questionNumber;
        question.questionText = questionText;
        question.explanation = explanation;
        question.difficulty = difficulty;
        question.options = new ArrayList<>();
        for (int i = 0; i < optionTexts.size(); i++) {
            int optionNumber = i + 1;
            question.options.add(QuizOption.create(optionNumber, optionTexts.get(i), optionNumber == correctOptionNumber));
        }
        return question;
    }

    // 난이도 미지정(null) 생성 편의 오버로드 — difficulty는 선택 값이다.
    public static QuizQuestion create(int questionNumber, String questionText, String explanation,
                                       int correctOptionNumber, List<String> optionTexts) {
        return create(questionNumber, questionText, explanation, null, correctOptionNumber, optionTexts);
    }

    public static QuizQuestion restore(Long id, int questionNumber, String questionText, String explanation,
                                        List<QuizOption> options) {
        return restore(id, questionNumber, questionText, explanation, null, options);
    }

    public static QuizQuestion restore(Long id, int questionNumber, String questionText, String explanation,
                                        Integer difficulty, List<QuizOption> options) {
        QuizQuestion question = new QuizQuestion();
        question.id = id;
        question.questionNumber = questionNumber;
        question.questionText = questionText;
        question.explanation = explanation;
        question.difficulty = difficulty;
        question.options = new ArrayList<>(options);
        return question;
    }

    public Long getId() { return id; }
    public int getQuestionNumber() { return questionNumber; }
    public String getQuestionText() { return questionText; }
    public String getExplanation() { return explanation; }
    public Integer getDifficulty() { return difficulty; }
    public List<QuizOption> getOptions() { return List.copyOf(options); }
}
