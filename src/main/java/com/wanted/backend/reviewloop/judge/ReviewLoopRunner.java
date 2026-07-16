package com.wanted.backend.reviewloop.judge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 범용 러너 — 두 가지 진입점으로 같은 루프 코어를 돌린다(재사용).
 *
 *  1) 도메인 스캔(수동): ./gradlew reviewLoop --args="--path src/.../domain/cart --domain cart --max 3"
 *  2) 변경 파일 게이트(pre-push 훅): ./gradlew reviewLoop --args="--files-from build/changed.txt --gate"
 *
 *   --path        디렉토리 스캔 (수동 리뷰용). --max로 파일 수 상한(비용 방어).
 *   --files-from  변경 파일 목록 파일(한 줄에 하나) — pre-push 훅이 diff로 만들어 넘긴다.
 *   --domain      해당 도메인 규칙만 적용(common + 도메인).
 *   --rules       규칙 카탈로그(기본 review-loop/rules.yaml).
 *   --gate        판정을 종료코드로 — 차단 결정(미완성/Critical)이 하나라도 있으면 exit 1.
 *
 * 코어(ReviewLoop 등)는 도메인·트리거 무관 — 여기선 대상·규칙만 주입한다.
 * GEMINI_API_KEY가 없으면 Gate 2(LLM 판정)는 생략하고 통과 처리한다(로컬 게이트는 훅의 Gate 1이 담당).
 */
public final class ReviewLoopRunner {

    public static void main(String[] args) throws Exception {
        String filesFrom = argVal(args, "--files-from", null);
        String path = argVal(args, "--path", null);
        String domain = argVal(args, "--domain", null);
        String rulesPath = argVal(args, "--rules", "review-loop/rules.yaml");
        int max = Integer.parseInt(argVal(args, "--max", "5"));
        boolean gate = hasFlag(args, "--gate");

        List<Path> targets = resolveTargets(filesFrom, path, max);
        if (targets == null) {
            System.out.println("사용법: --args=\"(--path <dir> | --files-from <list>) [--domain X] [--max N] [--gate]\"");
            return;
        }
        if (targets.isEmpty()) {
            System.out.println("리뷰할 .java 파일이 없습니다 → 통과.");
            return;   // exit 0
        }

        boolean hasKey = System.getenv("GEMINI_API_KEY") != null && !System.getenv("GEMINI_API_KEY").isBlank();
        if (!hasKey) {
            System.out.println("GEMINI_API_KEY 없음 → Gate 2(LLM 판정) 생략"
                    + (gate ? " · 게이트 통과 처리(Gate 1은 별도)" : ""));
            return;   // exit 0 — 키 부재로 push를 막지 않는다
        }

        RuleCatalog catalog = RuleCatalog.fromFile(Path.of(rulesPath));
        if (domain != null) {
            catalog = catalog.forDomain(domain);
        }
        LlmJudgePort judge = new GeminiJudgeAdapter();
        JudgeScorer scorer = new JudgeScorer(
                Map.of("CONV_001", 15, "PERF_001", 15, "ARCH_003a", 15),
                Map.of(Severity.MINOR, 10, Severity.CRITICAL, 40),
                JudgeScorer.DEFAULT_PASS_THRESHOLD);

        Files.createDirectories(Path.of("review-loop/logs"));
        AuditLogWriter audit = new AuditLogWriter(Path.of("review-loop/logs/error_log.jsonl"));

        StringBuilder out = new StringBuilder();
        out.append("== 리뷰 루프 실행 ==\n");
        out.append("대상  : ").append(targets.size()).append("개 파일")
           .append(gate ? " · 게이트 모드(차단 결정 시 exit 1)" : "").append('\n');
        out.append("도메인: ").append(domain == null ? "(전체 규칙)" : domain).append('\n');
        out.append("규칙  : judge 규칙 ").append(catalog.judgeRules().size()).append("개\n\n");

        int round = 0;
        boolean blocked = false;
        for (Path f : targets) {
            String code = Files.readString(f);
            Path parent = f.getParent() == null ? Path.of(".") : f.getParent();
            ReviewLoop loop = new ReviewLoop(judge, catalog, new EvidenceValidator(parent), scorer);
            JudgeVerdict v = loop.review(f.getFileName().toString(), code);

            if (isBlocking(v.decision())) {
                blocked = true;
            }
            out.append(mark(v.decision())).append(' ').append(f.getFileName())
               .append("  → score ").append(v.score()).append(" · ").append(v.decision())
               .append(" · findings ").append(v.findings().size()).append('\n');
            for (Finding fd : v.findings()) {
                out.append("    - ").append(fd.ruleId()).append(" (").append(fd.severity()).append("): ")
                   .append(fd.description()).append(" [").append(fd.file()).append(':').append(fd.line()).append("]\n");
            }
            audit.append(new AuditRecord(LocalDateTime.now().toString(), ++round, "gemini",
                    v.score(), v.hasCritical(), v.decision(), v.findings().size(), false));
        }
        out.append("\n감사 로그: review-loop/logs/error_log.jsonl (누적)\n");

        String report = out.toString();
        System.out.println(report);
        Files.writeString(Path.of("build/reviewloop-run.txt"), report);

        if (gate && blocked) {
            System.out.println("[GATE] 차단 결정(미완성/Critical) 발견 → exit 1. "
                    + "Minor·score<80은 통과. 우회: git push --no-verify");
            System.exit(1);
        }
    }

    /** 차단 결정 = 완료 하드게이트 미충족(INCOMPLETE) 또는 사람 승인 필요(AWAITING_HUMAN=Critical). */
    static boolean isBlocking(JudgeDecision d) {
        return d == JudgeDecision.INCOMPLETE || d == JudgeDecision.AWAITING_HUMAN;
    }

    /** 대상 목록 결정: --files-from 우선, 없으면 --path 스캔. 둘 다 없으면 null(사용법 출력). */
    private static List<Path> resolveTargets(String filesFrom, String path, int max) throws Exception {
        if (filesFrom != null) {
            Path listFile = Path.of(filesFrom);
            if (!Files.exists(listFile)) {   // 목록 파일 부재 → 크래시 대신 통과(리뷰 대상 없음)
                System.out.println("변경파일 목록이 없습니다: " + filesFrom + " → 통과.");
                return List.of();
            }
            List<Path> files = new ArrayList<>();
            for (String line : Files.readAllLines(listFile)) {
                String s = line.replace("﻿", "").trim();   // BOM/공백 방어
                if (s.isBlank() || !s.endsWith(".java")) {
                    continue;
                }
                Path p = Path.of(s);
                if (Files.exists(p)) {
                    files.add(p);
                }
            }
            if (files.size() > max) {   // --files-from에도 max 적용(LLM 호출 폭발·비용 방어). 조용히 자르지 않고 경고.
                System.out.println("변경 .java " + files.size() + "개 중 " + max + "개만 리뷰 · 나머지 "
                        + (files.size() - max) + "개 스킵 — 상한 조정: --max N");
                return List.copyOf(files.subList(0, max));
            }
            return files;
        }
        if (path != null) {
            try (Stream<Path> stream = Files.walk(Path.of(path))) {
                return stream.filter(p -> p.toString().endsWith(".java")).sorted().limit(max).toList();
            }
        }
        return null;
    }

    private static String mark(JudgeDecision d) {
        return switch (d) {
            case PASS -> "[OK ]";
            case NEEDS_REVISION -> "[FIX]";
            case AWAITING_HUMAN -> "[HUM]";
            case INCOMPLETE -> "[INC]";
        };
    }

    private static boolean hasFlag(String[] args, String key) {
        for (String a : args) {
            if (a.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String argVal(String[] args, String key, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return def;
    }

    private ReviewLoopRunner() {
    }
}
