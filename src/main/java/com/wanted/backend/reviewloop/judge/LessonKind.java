package com.wanted.backend.reviewloop.judge;

/**
 * 사람 검토에서 나온 교훈의 종류.
 * FALSE_POSITIVE : Judge가 위반이라 했지만 실제론 아니었음(오판) — 예: 투영은 예외인데 flag함
 * MISSED         : Judge가 놓친 진짜 위반 — 사람이 뒤늦게 발견
 */
public enum LessonKind {
    FALSE_POSITIVE,
    MISSED
}
