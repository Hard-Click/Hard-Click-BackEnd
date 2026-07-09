package com.wanted.backend.domain.quiz.application.query;

/**
 * 강사 퀴즈 통계 조회 조건.
 * keyword: 이름/아이디 검색어(null=전체), sort/filter: 수강생 목록 정렬·필터, page/size: 페이지네이션.
 */
public record QuizStatisticsQuery(
        Long instructorId,
        Long quizId,
        String keyword,
        SortType sort,
        FilterType filter,
        int page,
        int size
) {
    public enum SortType {
        SCORE_DESC, SCORE_ASC, NAME
    }

    public enum FilterType {
        ALL, SUBMITTED, NOT_SUBMITTED
    }
}
