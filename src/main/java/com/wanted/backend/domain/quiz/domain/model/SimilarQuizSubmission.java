package com.wanted.backend.domain.quiz.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 유사퀴즈(복습) 제출 기록 — 순수 도메인 모델(프레임워크 비의존).
 *
 * 정규 퀴즈와 달리 재응시를 허용한다(복습 루프). 채점 자체는 서비스에서 원문항 대조로 이뤄지고,
 * 이 모델은 그 결과(정오답·선택·풀이시간)를 시간순 이력으로 남기기 위한 것이다 — 추천기가
 * 라운드 단위로 난이도 사다리·시간 신호를 판정하는 근거가 된다.
 */
public class SimilarQuizSubmission {

    /** 문제당 체류 시간 상한(초). 범위 밖은 신뢰 불가 → 미측정(null)으로 취급(정규 퀴즈와 동일 정책). */
    private static final int MAX_TIME_SPENT_SECONDS = 3600;

    private Long id;
    private final Long similarQuizId;
    private final Long memberId;
    private final int score;
    private final int totalQuestionCount;
    private final int correctCount;
    private final LocalDateTime submittedAt;
    private final List<Answer> answers;

    private SimilarQuizSubmission(Long id, Long similarQuizId, Long memberId, int score,
                                 int totalQuestionCount, int correctCount, LocalDateTime submittedAt,
                                 List<Answer> answers) {
        this.id = id;
        this.similarQuizId = similarQuizId;
        this.memberId = memberId;
        this.score = score;
        this.totalQuestionCount = totalQuestionCount;
        this.correctCount = correctCount;
        this.submittedAt = submittedAt;
        this.answers = answers;
    }

    public static SimilarQuizSubmission create(Long similarQuizId, Long memberId, int score,
                                               int totalQuestionCount, int correctCount,
                                               LocalDateTime submittedAt, List<Answer> answers) {
        return new SimilarQuizSubmission(null, similarQuizId, memberId, score, totalQuestionCount,
                correctCount, submittedAt, new ArrayList<>(answers));
    }

    /** 영속화 이후 채번된 식별자를 반영해 도메인 객체를 복원한다(어댑터 save 반환용). */
    public static SimilarQuizSubmission restore(Long id, Long similarQuizId, Long memberId, int score,
                                                int totalQuestionCount, int correctCount,
                                                LocalDateTime submittedAt, List<Answer> answers) {
        return new SimilarQuizSubmission(id, similarQuizId, memberId, score, totalQuestionCount,
                correctCount, submittedAt, new ArrayList<>(answers));
    }

    public Long getId() { return id; }
    public Long getSimilarQuizId() { return similarQuizId; }
    public Long getMemberId() { return memberId; }
    public int getScore() { return score; }
    public int getTotalQuestionCount() { return totalQuestionCount; }
    public int getCorrectCount() { return correctCount; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public List<Answer> getAnswers() { return List.copyOf(answers); }

    public static class Answer {
        private final Long questionId;
        private final Integer selectedIndex;
        private final boolean correct;
        private final Integer timeSpentSeconds;

        private Answer(Long questionId, Integer selectedIndex, boolean correct, Integer timeSpentSeconds) {
            this.questionId = questionId;
            this.selectedIndex = selectedIndex;
            this.correct = correct;
            this.timeSpentSeconds = timeSpentSeconds;
        }

        public static Answer of(Long questionId, Integer selectedIndex, boolean correct, Integer timeSpentSeconds) {
            if (questionId == null) {
                throw new IllegalArgumentException("문항 식별자는 필수입니다.");
            }
            return new Answer(questionId, selectedIndex, correct, normalizeTimeSpent(timeSpentSeconds));
        }

        private static Integer normalizeTimeSpent(Integer seconds) {
            if (seconds == null || seconds < 0 || seconds > MAX_TIME_SPENT_SECONDS) {
                return null;
            }
            return seconds;
        }

        public Long getQuestionId() { return questionId; }
        public Integer getSelectedIndex() { return selectedIndex; }
        public boolean isCorrect() { return correct; }
        public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    }
}
