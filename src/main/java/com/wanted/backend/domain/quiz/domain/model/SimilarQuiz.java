package com.wanted.backend.domain.quiz.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 오답 기반 유사퀴즈 생성 세트(영속 애그리거트).
 * 문항은 기존 {@link QuizQuestion}을 참조하는 id 목록으로 고정한다(생성 시 노출 순서 보존).
 * 정답/해설은 원문항에서 조회하므로 이 애그리거트에 비정규화하지 않는다.
 * 재응시 없음 — 진입마다 새 세트를 생성한다.
 */
public class SimilarQuiz {

    private Long id;
    private Long memberId;
    private Long courseId;
    private Integer week;
    private String title;
    private List<Long> questionIds;
    private LocalDateTime createdAt;

    private SimilarQuiz() {}

    public static SimilarQuiz create(Long memberId, Long courseId, Integer week, String title,
                                     List<Long> questionIds) {
        if (memberId == null || courseId == null) {
            throw new IllegalArgumentException("회원/강의 식별자는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("유사퀴즈 제목은 필수입니다.");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("유사퀴즈에는 최소 1개의 문항이 필요합니다.");
        }
        SimilarQuiz quiz = new SimilarQuiz();
        quiz.memberId = memberId;
        quiz.courseId = courseId;
        quiz.week = week;
        quiz.title = title;
        quiz.questionIds = new ArrayList<>(questionIds);
        quiz.createdAt = LocalDateTime.now();
        return quiz;
    }

    public static SimilarQuiz restore(Long id, Long memberId, Long courseId, Integer week, String title,
                                      List<Long> questionIds, LocalDateTime createdAt) {
        SimilarQuiz quiz = new SimilarQuiz();
        quiz.id = id;
        quiz.memberId = memberId;
        quiz.courseId = courseId;
        quiz.week = week;
        quiz.title = title;
        quiz.questionIds = new ArrayList<>(questionIds);
        quiz.createdAt = createdAt;
        return quiz;
    }

    /** 제출(②) 시 본인 생성 세트만 채점하도록 소유자를 확인한다. 양쪽 null에 안전하다. */
    public boolean isOwnedBy(Long memberId) {
        return Objects.equals(this.memberId, memberId);
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getCourseId() { return courseId; }
    public Integer getWeek() { return week; }
    public String getTitle() { return title; }
    public List<Long> getQuestionIds() { return List.copyOf(questionIds); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
