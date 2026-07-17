package com.wanted.backend.reviewloop.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * findings 출력 방어 검증 — 게이트가 자기 IO 오류로 push를 막으면 안 된다.
 * LLM·판정은 여기 필요 없다 — 파일 쓰기와 경로 포맷만 본다.
 */
class ReviewLoopRunnerTest {

    @TempDir
    Path dir;

    @Test
    @DisplayName("부모 디렉터리가 없어도 findings를 쓴다 (NoSuchFileException 금지)")
    void createsMissingParentDirectories() throws IOException {
        Path out = dir.resolve("nope/deep/findings.txt");   // 부모 2단계가 없음

        ReviewLoopRunner.writeFindings(out.toString(), List.of("A.java:1 [CONV_001] 설명"));

        assertThat(out).exists();
        assertThat(Files.readString(out)).isEqualTo("A.java:1 [CONV_001] 설명\n");
    }

    @Test
    @DisplayName("파일명만 준 경우(부모 null) NPE 없이 쓴다")
    void writesWhenPathHasNoParent() throws IOException {
        Path bare = Path.of("reviewloop-findings-test.txt");
        assertThat(bare.getParent()).isNull();   // 가드 없으면 createDirectories(null) → NPE

        try {
            assertThatCode(() -> ReviewLoopRunner.writeFindings(bare.toString(), List.of()))
                    .doesNotThrowAnyException();
            assertThat(bare).exists();
        } finally {
            Files.deleteIfExists(bare);
        }
    }

    @Test
    @DisplayName("findings 0건이어도 빈 파일을 남긴다")
    void writesEmptyFileWhenNoFindings() throws IOException {
        Path out = dir.resolve("empty/findings.txt");

        ReviewLoopRunner.writeFindings(out.toString(), List.of());

        assertThat(out).exists();
        assertThat(Files.readString(out)).isEmpty();
    }

    @Test
    @DisplayName("findings 경로는 OS 무관하게 '/' 로 정규화된다")
    void normalizesPathSeparatorToSlash() {
        Path osSpecific = Path.of("src", "main", "java", "A.java");   // OS 구분자로 조립

        String posix = ReviewLoopRunner.toPosixPath(osSpecific);

        assertThat(posix).isEqualTo("src/main/java/A.java");
        assertThat(posix).doesNotContain("\\");
    }
}
