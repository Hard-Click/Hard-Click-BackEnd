package com.wanted.backend.domain.quiz.presentation.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate 1 · 시범 기능(퀴즈 난이도) API 경계 검증 — Bean Validation.
 * rules.yaml: QDIFF_001(난이도 필수·1~3), QDIFF_002(보기 4개·정답번호 1~4).
 * Spring을 띄우지 않고 Validator만으로 검증(빠른 로컬 게이트).
 */
class InstructorQuizRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private InstructorQuizRequest requestWith(Integer difficulty, int correctOptionNumber, int optionCount) {
        List<InstructorQuizRequest.Option> options = java.util.stream.IntStream.rangeClosed(1, optionCount)
                .mapToObj(i -> new InstructorQuizRequest.Option("보기" + i))
                .toList();
        InstructorQuizRequest.Question question = new InstructorQuizRequest.Question(
                "문제 내용", "해설", difficulty, correctOptionNumber, options);
        return new InstructorQuizRequest("퀴즈 제목", 1L, 1L, List.of(question));
    }

    private Set<String> messages(InstructorQuizRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("QDIFF_001: 난이도 2·정답 2·보기 4 → 위반 없음(유효)")
    void validRequestHasNoViolations() {
        assertThat(validator.validate(requestWith(2, 2, 4))).isEmpty();
    }

    @Test
    @DisplayName("QDIFF_001: 난이도 null(미입력)이면 '필수' 위반")
    void nullDifficultyIsRejected() {
        assertThat(messages(requestWith(null, 2, 4))).contains("난이도는 필수입니다.");
    }

    @Test
    @DisplayName("QDIFF_001: 난이도 0·4는 범위 위반")
    void outOfRangeDifficultyIsRejected() {
        assertThat(messages(requestWith(0, 2, 4))).contains("난이도는 1(하)~3(상) 사이여야 합니다.");
        assertThat(messages(requestWith(4, 2, 4))).contains("난이도는 1(하)~3(상) 사이여야 합니다.");
    }

    @Test
    @DisplayName("QDIFF_002: 정답 보기 번호가 1~4 밖이면 위반")
    void invalidCorrectOptionNumberIsRejected() {
        assertThat(messages(requestWith(2, 5, 4))).contains("정답 보기 번호는 1~4 사이여야 합니다.");
    }

    @Test
    @DisplayName("QDIFF_002: 보기가 4개가 아니면 위반")
    void wrongOptionCountIsRejected() {
        assertThat(messages(requestWith(2, 2, 3))).contains("보기는 4개가 필요합니다.");
    }
}
