package com.wanted.backend.reviewloop.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** reviewLesson 인자 파싱·검증 — 빈/잘못된 값이 lessons.jsonl을 오염시키지 않게 막는다. */
class ReviewLessonRecorderTest {

    @Test
    @DisplayName("정상 인자는 교훈으로 파싱된다 (rule·note는 trim)")
    void parsesValidArgs() {
        Lesson lesson = ReviewLessonRecorder.parse(new String[]{
                "--rule", " CONV_001 ", "--kind", "FALSE_POSITIVE", "--note", " 재사용 대상 없음 "});

        assertThat(lesson.ruleId()).isEqualTo("CONV_001");
        assertThat(lesson.kind()).isEqualTo(LessonKind.FALSE_POSITIVE);
        assertThat(lesson.humanNote()).isEqualTo("재사용 대상 없음");
    }

    @Test
    @DisplayName("빈 rule은 거부한다 (CodeRabbit 지적 — 빈 ruleId 기록 방지)")
    void rejectsBlankRule() {
        assertThatThrownBy(() -> ReviewLessonRecorder.parse(new String[]{
                "--rule", "  ", "--kind", "FALSE_POSITIVE", "--note", "x"}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 note는 거부한다")
    void rejectsBlankNote() {
        assertThatThrownBy(() -> ReviewLessonRecorder.parse(new String[]{
                "--rule", "CONV_001", "--kind", "FALSE_POSITIVE", "--note", "   "}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("잘못된 kind는 거부한다")
    void rejectsUnknownKind() {
        assertThatThrownBy(() -> ReviewLessonRecorder.parse(new String[]{
                "--rule", "CONV_001", "--kind", "BOGUS", "--note", "x"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FALSE_POSITIVE");
    }

    @Test
    @DisplayName("kind 누락도 거부한다")
    void rejectsMissingKind() {
        assertThatThrownBy(() -> ReviewLessonRecorder.parse(new String[]{
                "--rule", "CONV_001", "--note", "x"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
