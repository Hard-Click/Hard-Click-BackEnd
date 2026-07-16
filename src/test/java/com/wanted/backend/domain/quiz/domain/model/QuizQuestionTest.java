package com.wanted.backend.domain.quiz.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gate 1 · 시범 기능(퀴즈 난이도) 도메인 규칙 검증.
 * rules.yaml: QDIFF_002(보기 4개·정답번호), QDIFF_003(create/restore 공통 검증), QDIFF_004(DB 잘못된 값 차단).
 */
class QuizQuestionTest {

    private static final List<String> FOUR_OPTIONS = List.of("보기1", "보기2", "보기3", "보기4");

    private List<QuizOption> fourOptions() {
        return List.of(
                QuizOption.restore(1L, 1, "보기1", false),
                QuizOption.restore(2L, 2, "보기2", true),
                QuizOption.restore(3L, 3, "보기3", false),
                QuizOption.restore(4L, 4, "보기4", false));
    }

    // ── QDIFF_003: 난이도는 선택(null 허용), 값이 있으면 1~3 (create) ──

    @Test
    @DisplayName("QDIFF_003 create: 난이도 null(미지정) 허용")
    void createAllowsNullDifficulty() {
        QuizQuestion q = QuizQuestion.create(1, "문제", "해설", null, 2, FOUR_OPTIONS);
        assertThat(q.getDifficulty()).isNull();
    }

    @Test
    @DisplayName("QDIFF_003 create: 난이도 1·2·3 허용")
    void createAllowsDifficultyInRange() {
        for (int d = 1; d <= 3; d++) {
            int level = d;
            assertThatCode(() -> QuizQuestion.create(1, "문제", "해설", level, 2, FOUR_OPTIONS))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("QDIFF_003 create: 난이도 0·4는 예외")
    void createRejectsOutOfRangeDifficulty() {
        assertThatThrownBy(() -> QuizQuestion.create(1, "문제", "해설", 0, 2, FOUR_OPTIONS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("난이도");
        assertThatThrownBy(() -> QuizQuestion.create(1, "문제", "해설", 4, 2, FOUR_OPTIONS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("난이도");
    }

    // ── QDIFF_004: 복원(restore)도 동일 검증 — 잘못된 DB 값(0/4)이 응답으로 새지 않게 ──

    @Test
    @DisplayName("QDIFF_004 restore: 난이도 null 허용, 2 허용")
    void restoreAllowsNullAndValidDifficulty() {
        assertThat(QuizQuestion.restore(10L, 1, "문제", "해설", null, fourOptions()).getDifficulty()).isNull();
        assertThat(QuizQuestion.restore(10L, 1, "문제", "해설", 2, fourOptions()).getDifficulty()).isEqualTo(2);
    }

    @Test
    @DisplayName("QDIFF_004 restore: DB의 잘못된 값(0/4)은 예외로 차단")
    void restoreRejectsOutOfRangeDifficulty() {
        assertThatThrownBy(() -> QuizQuestion.restore(10L, 1, "문제", "해설", 0, fourOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("난이도");
        assertThatThrownBy(() -> QuizQuestion.restore(10L, 1, "문제", "해설", 4, fourOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("난이도");
    }

    // ── QDIFF_002: 보기 4개·정답번호 범위 ──

    @Test
    @DisplayName("QDIFF_002 create: 보기가 4개가 아니면 예외")
    void createRejectsWrongOptionCount() {
        assertThatThrownBy(() -> QuizQuestion.create(1, "문제", "해설", 2, 2, List.of("보기1", "보기2", "보기3")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("QDIFF_002 create: 정답 보기 번호가 범위 밖이면 예외")
    void createRejectsInvalidCorrectOptionNumber() {
        assertThatThrownBy(() -> QuizQuestion.create(1, "문제", "해설", 2, 0, FOUR_OPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizQuestion.create(1, "문제", "해설", 2, 5, FOUR_OPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("QDIFF_002 create: 정답 번호 위치의 보기만 correct=true")
    void createMarksOnlyCorrectOption() {
        QuizQuestion q = QuizQuestion.create(1, "문제", "해설", 2, 2, FOUR_OPTIONS);
        assertThat(q.getOptions()).filteredOn(QuizOption::isCorrect)
                .singleElement()
                .extracting(QuizOption::getOptionNumber).isEqualTo(2);
    }
}
