package com.wanted.backend.reviewloop.judge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Learning Loop 쓰기 끝 — 사람/드라이버의 판정을 교훈으로 기록한다.
 *
 * <p>드라이버(Claude Code)가 요청서의 finding을 사용자와 확정한 뒤 호출한다:
 * <ul>
 *   <li>사용자가 "건너뛰기(오탐)"로 판정 → {@code --kind FALSE_POSITIVE}
 *   <li>Judge가 놓친 걸 사람이 뒤늦게 발견 → {@code --kind MISSED}
 * </ul>
 * 기록된 교훈은 다음 판정부터 {@link ReviewLoop}이 프롬프트에 실어 반영한다(루프가 닫힌다).
 * 반대로 사용자가 방안을 골라 수정하면(=Judge가 옳았음) 기록하지 않는다.
 *
 * <p>실행: {@code ./gradlew reviewLesson --args="--rule CONV_001 --kind FALSE_POSITIVE --note '재사용할 유틸이 없어 오탐'"}
 */
public final class ReviewLessonRecorder {

    private static final Path LESSONS = Path.of("review-loop/logs/lessons.jsonl");

    public static void main(String[] args) throws IOException {
        String rule = argVal(args, "--rule", null);
        String kindArg = argVal(args, "--kind", null);
        String note = argVal(args, "--note", null);

        if (rule == null || kindArg == null || note == null || note.isBlank()) {
            System.out.println("사용법: --args=\"--rule <RULE_ID> --kind <FALSE_POSITIVE|MISSED> --note <한 줄 근거>\"");
            System.exit(2);
            return;
        }

        LessonKind kind;
        try {
            kind = LessonKind.valueOf(kindArg);
        } catch (IllegalArgumentException e) {
            System.out.println("--kind 는 FALSE_POSITIVE 또는 MISSED 여야 합니다: " + kindArg);
            System.exit(2);
            return;
        }

        Files.createDirectories(LESSONS.getParent());
        Lesson lesson = new Lesson(LocalDateTime.now().toString(), rule, kind, note.strip());
        new KnowledgeStore(LESSONS).record(lesson);

        System.out.println("[reviewLesson] 교훈 기록 → " + LESSONS);
        System.out.println("  [" + kind + "] " + rule + " — " + lesson.humanNote());
        System.out.println("  다음 판정부터 프롬프트에 반영됩니다.");
    }

    private static String argVal(String[] args, String key, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return def;
    }

    private ReviewLessonRecorder() {
    }
}
