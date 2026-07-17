package com.wanted.backend.reviewloop.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 규칙 정확도 집계 — 규칙별 오탐/누락 카운트와 정렬(오탐 많은 순). */
class RuleAccuracyTest {

    private Lesson lesson(String rule, LessonKind kind) {
        return new Lesson("2026-07-18T00:00:00", rule, kind, "note");
    }

    @Test
    @DisplayName("규칙별로 FALSE_POSITIVE·MISSED를 센다")
    void countsByRuleAndKind() {
        List<RuleAccuracy.Stat> stats = RuleAccuracy.summarize(List.of(
                lesson("CONV_001", LessonKind.FALSE_POSITIVE),
                lesson("CONV_001", LessonKind.FALSE_POSITIVE),
                lesson("PERF_001", LessonKind.MISSED)));

        RuleAccuracy.Stat conv = stats.stream().filter(s -> s.ruleId().equals("CONV_001")).findFirst().orElseThrow();
        RuleAccuracy.Stat perf = stats.stream().filter(s -> s.ruleId().equals("PERF_001")).findFirst().orElseThrow();

        assertThat(conv.falsePositives()).isEqualTo(2);
        assertThat(conv.missed()).isZero();
        assertThat(perf.falsePositives()).isZero();
        assertThat(perf.missed()).isEqualTo(1);
        assertThat(perf.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("오탐이 많은 규칙이 먼저 온다 (프롬프트 개선 우선순위)")
    void sortsByFalsePositivesDescending() {
        List<RuleAccuracy.Stat> stats = RuleAccuracy.summarize(List.of(
                lesson("PERF_001", LessonKind.FALSE_POSITIVE),
                lesson("CONV_001", LessonKind.FALSE_POSITIVE),
                lesson("CONV_001", LessonKind.FALSE_POSITIVE)));

        assertThat(stats).extracting(RuleAccuracy.Stat::ruleId).containsExactly("CONV_001", "PERF_001");
    }

    @Test
    @DisplayName("교훈이 없으면 빈 신호 메시지")
    void emptyWhenNoLessons() {
        assertThat(RuleAccuracy.summarize(List.of())).isEmpty();
        assertThat(RuleAccuracy.render(List.of())).contains("아직 신호 없음");
    }
}
