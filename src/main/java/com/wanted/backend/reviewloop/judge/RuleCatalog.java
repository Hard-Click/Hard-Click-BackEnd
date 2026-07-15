package com.wanted.backend.reviewloop.judge;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * rules.yaml(카탈로그 SSOT) 로더 — Judge가 맡는 의미규칙(enforced_by=judge)과 NOTE(비채점 인지)만 추린다.
 * 여기서 뽑은 규칙이 JudgePromptBuilder를 통해 Judge 프롬프트가 된다 → 규칙은 rules.yaml 한 곳에서만 관리.
 */
public class RuleCatalog {

    /** Judge가 채점하는 의미규칙. domain="common"이면 전 도메인 공통, 아니면 해당 도메인 전용. */
    public record JudgeRule(String id, String text, Severity severity, String domain) {}

    /** 비채점 인지 항목(의도적 결정) — Judge가 flag하면 오판. */
    public record Note(String id, String text) {}

    private final List<JudgeRule> judgeRules;
    private final List<Note> notes;
    private final Map<String, Severity> judgeSeverityById;   // ruleId → 카탈로그 severity (정규화용)

    private RuleCatalog(List<JudgeRule> judgeRules, List<Note> notes) {
        this.judgeRules = List.copyOf(judgeRules);
        this.notes = List.copyOf(notes);
        Map<String, Severity> sev = new HashMap<>();
        for (JudgeRule r : judgeRules) {
            sev.put(r.id(), r.severity());
        }
        this.judgeSeverityById = Map.copyOf(sev);
    }

    public List<JudgeRule> judgeRules() {
        return judgeRules;
    }

    public List<Note> notes() {
        return notes;
    }

    /**
     * 특정 도메인용 카탈로그 — common(공통) + 그 도메인 전용 규칙만 남긴다.
     * 도메인 owner가 "내 도메인 것만" 루프를 돌릴 수 있게 한다.
     */
    public RuleCatalog forDomain(String domain) {
        List<JudgeRule> filtered = judgeRules.stream()
                .filter(r -> "common".equalsIgnoreCase(r.domain()) || r.domain().equalsIgnoreCase(domain))
                .toList();
        return new RuleCatalog(filtered, notes);
    }

    /**
     * Judge finding을 카탈로그로 정규화한다(채점 직전):
     *  - 카탈로그에 없는 ruleId(LLM 환각)는 제거한다 — Judge는 카탈로그 규칙만 인용할 수 있다(화이트리스트).
     *  - severity는 LLM 출력이 아니라 카탈로그 값으로 덮어쓴다 — 치명적(CRITICAL) 판정을 LLM 변덕에 맡기지 않는다.
     * 이로써 "MINOR 규칙을 LLM이 CRITICAL로 잘못 매겨 사람 승인으로 오라우팅"되는 경로가 닫힌다.
     */
    public List<Finding> normalize(List<Finding> findings) {
        List<Finding> out = new ArrayList<>();
        for (Finding f : findings) {
            Severity catalogSeverity = judgeSeverityById.get(f.ruleId());
            if (catalogSeverity == null) {
                continue;   // 카탈로그에 없는 규칙 = 환각, 채점 제외
            }
            out.add(f.severity() == catalogSeverity ? f : f.withSeverity(catalogSeverity));
        }
        return out;
    }

    public static RuleCatalog fromFile(Path path) throws IOException {
        return fromYaml(Files.readString(path));
    }

    @SuppressWarnings("unchecked")
    public static RuleCatalog fromYaml(String yaml) {
        Map<String, Object> root = new Yaml().load(yaml);

        List<JudgeRule> judge = new ArrayList<>();
        for (Map<String, Object> rule : asList(root.get("rules"))) {
            if (isJudge(rule.get("enforced_by"))) {
                judge.add(new JudgeRule(
                        str(rule.get("id")),
                        str(rule.get("text")),
                        parseSeverity(rule.get("severity")),
                        rule.get("domain") == null ? "common" : str(rule.get("domain"))));
            }
        }

        List<Note> notes = new ArrayList<>();
        for (Map<String, Object> note : asList(root.get("notes"))) {
            notes.add(new Note(str(note.get("id")), str(note.get("text"))));
        }

        return new RuleCatalog(judge, notes);
    }

    private static boolean isJudge(Object enforcedBy) {
        if (enforcedBy instanceof String s) {
            return s.equals("judge");
        }
        if (enforcedBy instanceof List<?> list) {
            return list.contains("judge");
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static Severity parseSeverity(Object raw) {
        try {
            return Severity.valueOf(str(raw).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.MINOR;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
