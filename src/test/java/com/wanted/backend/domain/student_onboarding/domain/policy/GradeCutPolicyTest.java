package com.wanted.backend.domain.student_onboarding.domain.policy;

import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradeCutPolicyTest {

    /** 영어는 절대평가 - 등급컷이 고정이라 경계값을 확정 검증할 수 있다. */
    @ParameterizedTest
    @CsvSource({
            "100, 1", "90, 1", "89, 2", "80, 2", "79, 3",
            "60, 4", "50, 5", "40, 6", "30, 7", "20, 8", "19, 9", "0, 9"
    })
    void convertsEnglishByAbsoluteCuts(int rawScore, int expectedGrade) {
        assertThat(GradeCutPolicy.toGrade(SubjectArea.ENGLISH, rawScore)).isEqualTo(expectedGrade);
    }

    /** 한국사도 절대평가 - 50점 만점에 5점 간격. */
    @ParameterizedTest
    @CsvSource({
            "50, 1", "40, 1", "39, 2", "35, 2", "34, 3",
            "20, 5", "10, 7", "5, 8", "4, 9", "0, 9"
    })
    void convertsHistoryByAbsoluteCuts(int rawScore, int expectedGrade) {
        assertThat(GradeCutPolicy.toGrade(SubjectArea.HISTORY, rawScore)).isEqualTo(expectedGrade);
    }

    @Test
    void givesTopGradeForPerfectScoreInEveryArea() {
        for (SubjectArea area : SubjectArea.values()) {
            assertThat(GradeCutPolicy.toGrade(area, area.getMaxRawScore()))
                    .as("%s 만점", area)
                    .isEqualTo(1);
        }
    }

    @Test
    void givesLowestGradeForZeroInEveryArea() {
        for (SubjectArea area : SubjectArea.values()) {
            assertThat(GradeCutPolicy.toGrade(area, 0))
                    .as("%s 0점", area)
                    .isEqualTo(9);
        }
    }

    @Test
    void gradesNeverIncreaseAsRawScoreDrops() {
        for (SubjectArea area : SubjectArea.values()) {
            int previous = 1;
            for (int score = area.getMaxRawScore(); score >= 0; score--) {
                int grade = GradeCutPolicy.toGrade(area, score);
                assertThat(grade)
                        .as("%s %d점 - 원점수가 낮아지는데 등급이 좋아지면 안 된다", area, score)
                        .isGreaterThanOrEqualTo(previous);
                previous = grade;
            }
        }
    }

    /** 탐구·한국사는 50점 만점이라 51점은 입력 오류다. 영역별 만점이 다른 게 이 검증의 이유. */
    @Test
    void rejectsRawScoreAboveAreaMaximum() {
        assertThatThrownBy(() -> GradeCutPolicy.toGrade(SubjectArea.HISTORY, 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0~50");

        assertThatThrownBy(() -> GradeCutPolicy.toGrade(SubjectArea.EXPLORATION_1, 51))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GradeCutPolicy.toGrade(SubjectArea.KOREAN, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0~100");
    }

    @Test
    void rejectsNegativeRawScore() {
        assertThatThrownBy(() -> GradeCutPolicy.toGrade(SubjectArea.KOREAN, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
