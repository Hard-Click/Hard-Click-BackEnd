package com.wanted.backend.domain.student_onboarding.domain.model;

/**
 * 수능 응시영역. 온보딩 3단계(최근 모의고사 성적) 입력 단위이며 student_exam_score.subject_area 에 저장된다.
 *
 * <p>영어/한국사는 절대평가(원점수 구간 = 등급)이고 나머지는 상대평가다 - {@link com.wanted.backend.domain.student_onboarding.domain.policy.GradeCutPolicy} 참고.
 */
public enum SubjectArea {

    /** 국어 - 선택과목 있음(화법과 작문 / 언어와 매체). 100점 만점, 상대평가. */
    KOREAN(100, false),

    /** 수학 - 선택과목 있음(미적분 / 기하 / 확률과 통계). 100점 만점, 상대평가. */
    MATH(100, false),

    /** 영어 - 선택과목 없음. 100점 만점, 절대평가. */
    ENGLISH(100, true),

    /** 한국사 - 선택과목 없음. 50점 만점, 절대평가. */
    HISTORY(50, true),

    /** 탐구 1 - 선택과목 있음. 50점 만점, 상대평가. */
    EXPLORATION_1(50, false),

    /** 탐구 2 - 선택과목 있음. 50점 만점, 상대평가. */
    EXPLORATION_2(50, false);

    private final int maxRawScore;
    private final boolean absoluteGrading;

    SubjectArea(int maxRawScore, boolean absoluteGrading) {
        this.maxRawScore = maxRawScore;
        this.absoluteGrading = absoluteGrading;
    }

    public int getMaxRawScore() {
        return maxRawScore;
    }

    public boolean isAbsoluteGrading() {
        return absoluteGrading;
    }

    /** 영역 안에서 응시과목을 고르는지 여부. false 면 subject_name 은 NULL 이다. */
    public boolean hasElective() {
        return this != ENGLISH && this != HISTORY;
    }
}
