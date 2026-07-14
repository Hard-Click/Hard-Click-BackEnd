package com.wanted.backend.domain.quiz.application.query;

/**
 * 강사/관리자 퀴즈 통계 조회 조건.
 * keyword: 이름/아이디 검색어(null=전체), sort/filter: 수강생 목록 정렬·필터, page/size: 페이지네이션.
 * instructorId는 강사 조회의 소유권 검증용이며, 관리자(ADMIN) 조회에서는 사용하지 않는다(null 허용).
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
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    public enum SortType {
        SCORE_DESC, SCORE_ASC, NAME
    }

    public enum FilterType {
        ALL, SUBMITTED, NOT_SUBMITTED
    }

    // 원시 요청 파라미터(문자열/정수)를 파싱·정규화하여 쿼리를 만든다. 강사/관리자 컨트롤러 공용.
    public static QuizStatisticsQuery of(Long instructorId, Long quizId, String keyword,
                                         String sort, String filter, Integer page, Integer size) {
        return new QuizStatisticsQuery(instructorId, quizId, keyword,
                parseSort(sort), parseFilter(filter), normalizePage(page), normalizeSize(size));
    }

    private static SortType parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SortType.SCORE_DESC;
        }
        try {
            return SortType.valueOf(sort.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SortType.SCORE_DESC;
        }
    }

    private static FilterType parseFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return FilterType.ALL;
        }
        try {
            return FilterType.valueOf(filter.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FilterType.ALL;
        }
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
