package com.wanted.backend.reviewloop.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 지식 축적 산출물을 고정 위치(review-loop/logs/)에 실제로 남기는 데모 생성기.
 * 실행: ./gradlew test --tests "*KnowledgeArtifactsDemo"
 * 생성물(사람이 열어볼 수 있음): error_log.jsonl(라운드 감사) · lessons.jsonl(사람 교훈) · report.md(산문)
 */
class KnowledgeArtifactsDemo {

    @Test
    @DisplayName("지식 축적 산출물을 review-loop/logs/에 생성")
    void generate() throws IOException {
        Path dir = Path.of("review-loop/logs");
        Files.createDirectories(dir);
        Path errorLog = dir.resolve("error_log.jsonl");
        Path lessonsLog = dir.resolve("lessons.jsonl");
        Files.deleteIfExists(errorLog);
        Files.deleteIfExists(lessonsLog);

        // 1) 라운드 감사 로그(무슨 일이 있었나) — 자동수정 루프가 남기는 기록
        AuditLogWriter audit = new AuditLogWriter(errorLog);
        audit.append(new AuditRecord("2026-07-15T12:00:00", 1, "gemini-flash-latest",
                75, false, JudgeDecision.NEEDS_REVISION, 1, false));
        audit.append(new AuditRecord("2026-07-15T12:00:40", 2, "gemini-flash-latest",
                100, false, JudgeDecision.PASS, 0, false));

        // 2) 사람 교훈(정정) — HITL 학습 데이터
        KnowledgeStore knowledge = new KnowledgeStore(lessonsLog);
        knowledge.record(new Lesson("2026-07-15T12:05:00", "ARCH_003a", LessonKind.FALSE_POSITIVE,
                "read-model 투영은 예외 — flag 금지"));
        knowledge.record(new Lesson("2026-07-15T13:10:00", "PERF_001", LessonKind.MISSED,
                "상위 서비스 루프 내 조회를 놓침 — 호출부까지 함께 볼 것"));

        // 3) 산문 md(사람용 리포트) — error_log + 교훈을 합쳐 렌더
        Files.writeString(dir.resolve("report.md"), ReviewReport.fromFiles(errorLog, lessonsLog));
    }
}
