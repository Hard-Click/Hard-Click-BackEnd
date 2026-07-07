package com.wanted.backend.domain.quiz.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Quiz {

    private Long id;
    private Long instructorId;
    private Long courseId;
    private Long sectionId;
    private String title;
    private List<QuizQuestion> questions;
    private LocalDateTime createdAt;

    private Quiz() {}

    public static Quiz create(Long instructorId, Long courseId, Long sectionId, String title,
                               List<QuizQuestion> questions) {
        if (instructorId == null || courseId == null || sectionId == null) {
            throw new IllegalArgumentException("강사/강의/섹션 식별자는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("퀴즈 제목은 필수입니다.");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("퀴즈에는 최소 1개의 문항이 필요합니다.");
        }

        Quiz quiz = new Quiz();
        quiz.instructorId = instructorId;
        quiz.courseId = courseId;
        quiz.sectionId = sectionId;
        quiz.title = title;
        quiz.questions = new ArrayList<>(questions);
        quiz.createdAt = LocalDateTime.now();
        return quiz;
    }

    public static Quiz restore(Long id, Long instructorId, Long courseId, Long sectionId, String title,
                                List<QuizQuestion> questions, LocalDateTime createdAt) {
        Quiz quiz = new Quiz();
        quiz.id = id;
        quiz.instructorId = instructorId;
        quiz.courseId = courseId;
        quiz.sectionId = sectionId;
        quiz.title = title;
        quiz.questions = new ArrayList<>(questions);
        quiz.createdAt = createdAt;
        return quiz;
    }

    public Long getId() { return id; }
    public Long getInstructorId() { return instructorId; }
    public Long getCourseId() { return courseId; }
    public Long getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public List<QuizQuestion> getQuestions() { return List.copyOf(questions); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
