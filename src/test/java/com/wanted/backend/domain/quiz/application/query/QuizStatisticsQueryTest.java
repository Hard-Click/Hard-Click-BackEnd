package com.wanted.backend.domain.quiz.application.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * of() 정적 팩토리의 파싱·정규화 분기를 직접 검증한다(강사·관리자 컨트롤러 공용 로직).
 */
class QuizStatisticsQueryTest {

    private QuizStatisticsQuery build(String sort, String filter, Integer page, Integer size) {
        return QuizStatisticsQuery.of(1L, 90L, null, sort, filter, page, size);
    }

    @Test
    void parsesValidSortAndFilterCaseInsensitively() {
        QuizStatisticsQuery q = build("score_asc", "submitted", 2, 20);

        assertThat(q.sort()).isEqualTo(QuizStatisticsQuery.SortType.SCORE_ASC);
        assertThat(q.filter()).isEqualTo(QuizStatisticsQuery.FilterType.SUBMITTED);
        assertThat(q.page()).isEqualTo(2);
        assertThat(q.size()).isEqualTo(20);
    }

    @Test
    void fallsBackToDefaultsForNullOrBlankSortAndFilter() {
        assertThat(build(null, null, 0, 10).sort()).isEqualTo(QuizStatisticsQuery.SortType.SCORE_DESC);
        assertThat(build("  ", "  ", 0, 10).filter()).isEqualTo(QuizStatisticsQuery.FilterType.ALL);
    }

    @Test
    void fallsBackToDefaultsForUnknownSortAndFilter() {
        assertThat(build("bogus", "nope", 0, 10).sort()).isEqualTo(QuizStatisticsQuery.SortType.SCORE_DESC);
        assertThat(build("bogus", "nope", 0, 10).filter()).isEqualTo(QuizStatisticsQuery.FilterType.ALL);
    }

    @Test
    void normalizesPageToZeroWhenNullOrNegative() {
        assertThat(build(null, null, null, 10).page()).isZero();
        assertThat(build(null, null, -5, 10).page()).isZero();
    }

    @Test
    void normalizesSizeToDefaultWhenNullOrNonPositiveAndCapsAtMax() {
        assertThat(build(null, null, 0, null).size()).isEqualTo(10);  // 기본
        assertThat(build(null, null, 0, 0).size()).isEqualTo(10);     // <=0 → 기본
        assertThat(build(null, null, 0, 999).size()).isEqualTo(50);   // MAX 상한
        assertThat(build(null, null, 0, 30).size()).isEqualTo(30);    // 정상값 유지
    }

    @Test
    void keepsInstructorIdNullableForAdminUsage() {
        QuizStatisticsQuery q = QuizStatisticsQuery.of(null, 90L, "kim", "name", null, 1, 10);

        assertThat(q.instructorId()).isNull();
        assertThat(q.quizId()).isEqualTo(90L);
        assertThat(q.keyword()).isEqualTo("kim");
        assertThat(q.sort()).isEqualTo(QuizStatisticsQuery.SortType.NAME);
    }
}
