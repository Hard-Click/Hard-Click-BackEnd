package com.wanted.backend.reviewloop.judge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙 정확도 신호 — 축적된 교훈을 규칙별로 집계해 "어떤 규칙이 오탐/누락을 많이 냈나"를 보여준다.
 * Learning Loop의 마지막 노드(Rule Accuracy → Prompt 개선): 오탐 지목이 잦은 규칙 = 프롬프트 손볼 후보.
 *
 * <p><b>주의</b>: 이건 "지목 횟수"지 진짜 비율(%)이 아니다. 오탐률 = 오탐 ÷ (그 규칙의 전체 지적 횟수)인데,
 * 분모(규칙별 총 지적 횟수)는 lessons.jsonl에 없다(감사 로그를 규칙별로 쪼개야 가능 — 후속 과제).
 *
 * <p>실행: {@code ./gradlew reviewAccuracy}
 */
public final class RuleAccuracy {

    /** 규칙 하나의 교훈 집계. */
    public record Stat(String ruleId, long falsePositives, long missed) {
        public long total() {
            return falsePositives + missed;
        }
    }

    /** 교훈을 규칙별로 집계 — 오탐 많은 순, 동수면 ruleId 순. */
    static List<Stat> summarize(List<Lesson> lessons) {
        Map<String, long[]> byRule = new LinkedHashMap<>();   // ruleId -> [falsePositives, missed]
        for (Lesson l : lessons) {
            long[] c = byRule.computeIfAbsent(l.ruleId(), k -> new long[2]);
            if (l.kind() == LessonKind.FALSE_POSITIVE) {
                c[0]++;
            } else if (l.kind() == LessonKind.MISSED) {
                c[1]++;
            }
        }
        List<Stat> stats = new ArrayList<>();
        byRule.forEach((rule, c) -> stats.add(new Stat(rule, c[0], c[1])));
        stats.sort(Comparator.comparingLong(Stat::falsePositives).reversed()
                .thenComparing(Stat::ruleId));
        return stats;
    }

    /** 사람이 읽는 표. */
    static String render(List<Stat> stats) {
        if (stats.isEmpty()) {
            return "규칙 정확도: 축적된 교훈 없음 — 아직 신호 없음.\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("규칙 정확도 (교훈 기반 지목 횟수 · 오탐 많은 순)\n");
        sb.append(String.format("%-14s %14s %8s%n", "RULE", "FALSE_POSITIVE", "MISSED"));
        for (Stat s : stats) {
            sb.append(String.format("%-14s %14d %8d%n", s.ruleId(), s.falsePositives(), s.missed()));
        }
        sb.append("\n오탐 지목이 잦은 규칙 = 판정 프롬프트를 손볼 후보(false positive를 줄이도록).\n");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        List<Lesson> lessons = new KnowledgeStore(ReviewLoopPaths.LESSONS).lessons();
        System.out.print(render(summarize(lessons)));
    }

    private RuleAccuracy() {
    }
}
