package com.wanted.backend.reviewloop.judge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 범용 러너 — 도메인 owner가 자기 경로를 주면 그 경로의 실제 파일에 루프를 돌린다(재사용 진입점).
 *
 * 실행: ./gradlew reviewLoop --args="--path src/main/java/com/wanted/backend/domain/cart --domain cart --max 3"
 *   --path   (필수) 리뷰할 디렉토리
 *   --domain (선택) 그 도메인 규칙만 적용 (common + 해당 도메인)
 *   --rules  (선택) 규칙 카탈로그 (기본 review-loop/rules.yaml)
 *   --max    (선택) 리뷰할 파일 수 상한 (기본 5, 비용 방어)
 *
 * 코어(ReviewLoop 등)는 도메인 무관 — 여기선 경로·규칙만 주입한다.
 */
public final class ReviewLoopRunner {

    public static void main(String[] args) throws Exception {
        String path = argVal(args, "--path", null);
        String domain = argVal(args, "--domain", null);
        String rulesPath = argVal(args, "--rules", "review-loop/rules.yaml");
        int max = Integer.parseInt(argVal(args, "--max", "5"));

        if (path == null) {
            System.out.println("사용법: --args=\"--path <dir> [--domain X] [--max N] [--rules f]\"");
            return;
        }
        if (System.getenv("GEMINI_API_KEY") == null || System.getenv("GEMINI_API_KEY").isBlank()) {
            System.out.println("GEMINI_API_KEY 환경변수가 필요합니다.");
            return;
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

        List<Path> allJava;
        try (Stream<Path> stream = Files.walk(Path.of(path))) {
            allJava = stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        List<Path> targets = allJava.stream().limit(max).toList();

        Files.createDirectories(Path.of("review-loop/logs"));
        AuditLogWriter audit = new AuditLogWriter(Path.of("review-loop/logs/error_log.jsonl"));

        StringBuilder out = new StringBuilder();
        out.append("== 리뷰 루프 실행 ==\n");
        out.append("경로  : ").append(path).append('\n');
        out.append("도메인: ").append(domain == null ? "(전체 규칙)" : domain).append('\n');
        out.append("규칙  : judge 규칙 ").append(catalog.judgeRules().size()).append("개\n");
        out.append("파일  : ").append(allJava.size()).append("개 발견, ").append(targets.size()).append("개 리뷰");
        if (allJava.size() > targets.size()) {
            out.append(" (나머지 ").append(allJava.size() - targets.size()).append("개는 --max로 조절)");
        }
        out.append("\n\n");

        int round = 0;
        for (Path f : targets) {
            String code = Files.readString(f);
            // 파일별 EvidenceValidator(부모 기준) — finding의 file:line 근거 검증
            ReviewLoop loop = new ReviewLoop(judge, catalog, new EvidenceValidator(f.getParent()), scorer);
            JudgeVerdict v = loop.review(f.getFileName().toString(), code);

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
    }

    private static String mark(JudgeDecision d) {
        return switch (d) {
            case PASS -> "[OK ]";
            case NEEDS_REVISION -> "[FIX]";
            case AWAITING_HUMAN -> "[HUM]";
            case INCOMPLETE -> "[INC]";
        };
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
