package com.wanted.backend.domain.student_onboarding.domain.policy;

import com.wanted.backend.domain.student_onboarding.domain.model.SubjectArea;

/**
 * 모의고사 원점수 -> 등급(1~9) 변환.
 *
 * <p>화면은 원점수를 받고 student_diagnostic_score/student_exam_score 는 등급을 요구하므로 그 사이를 메운다.
 * 원점수는 student_exam_score.raw_score 에 함께 보관해 등급컷이 바뀌면 재계산할 수 있게 한다.
 *
 * <p>화면이 원점수를 받고 서버가 변환하는 구조는 팀 결정이다(2026-07-15).
 * "학생에게 등급을 직접 고르게 하면 변환이 필요 없다"는 방향은 검토 후 채택하지 않았다 — 되돌리지 말 것.
 *
 * <h2>⚠️ 상대평가 영역의 등급컷은 확정 데이터가 아니다</h2>
 * 영어·한국사는 절대평가라 등급컷이 고정이고 아래 값이 그대로 맞다.
 * 그러나 <b>국어·수학·탐구는 상대평가</b>라 등급컷이 시행마다 바뀌며, 아래 값은 실제 시행 데이터가
 * 아니라 <b>임시 placeholder</b>다. 실제 등급컷 데이터는 별도로 확보해 교체하기로 했고, 시행별로
 * 달라져야 한다면 이 상수 테이블을 DB(등급컷 테이블 + 시행 식별자)로 옮겨야 한다.
 * 교체 전까지 이 값에 근거해 학생에게 등급을 확정 고지하면 안 된다.
 * 원점수를 student_exam_score.raw_score 에 남겨두므로 등급컷 확보 후 재계산이 가능하다.
 */
public final class GradeCutPolicy {

    /**
     * 영어 절대평가: 90점 이상 1등급, 이후 10점 간격. (고정 - 실제 기준)
     * 인덱스 i = 등급 (i+1) 의 최저 원점수.
     */
    private static final int[] ENGLISH_CUTS = {90, 80, 70, 60, 50, 40, 30, 20};

    /**
     * 한국사 절대평가(50점 만점): 40점 이상 1등급, 이후 5점 간격. (고정 - 실제 기준)
     */
    private static final int[] HISTORY_CUTS = {40, 35, 30, 25, 20, 15, 10, 5};

    /** ⚠️ placeholder - 국어/수학(100점 만점) 상대평가 임시 등급컷. 실제 시행 데이터로 교체 필요. */
    private static final int[] RELATIVE_100_CUTS = {96, 89, 80, 68, 54, 40, 27, 16};

    /** ⚠️ placeholder - 탐구(50점 만점) 상대평가 임시 등급컷. 실제 시행 데이터로 교체 필요. */
    private static final int[] RELATIVE_50_CUTS = {47, 44, 40, 34, 27, 20, 14, 9};

    private static final int LOWEST_GRADE = 9;

    private GradeCutPolicy() {
    }

    /**
     * 원점수를 등급으로 변환한다.
     *
     * @param area     응시영역 - 만점·평가방식이 여기서 갈린다
     * @param rawScore 원점수 (0 ~ area.getMaxRawScore())
     * @return 등급 1~9
     * @throws IllegalArgumentException 원점수가 해당 영역 만점을 넘거나 음수인 경우
     */
    public static int toGrade(SubjectArea area, int rawScore) {
        if (rawScore < 0 || rawScore > area.getMaxRawScore()) {
            throw new IllegalArgumentException(
                    "원점수가 %s 영역 범위(0~%d)를 벗어났습니다: %d"
                            .formatted(area.name(), area.getMaxRawScore(), rawScore));
        }

        int[] cuts = cutsFor(area);
        for (int i = 0; i < cuts.length; i++) {
            if (rawScore >= cuts[i]) {
                return i + 1;
            }
        }
        return LOWEST_GRADE;
    }

    private static int[] cutsFor(SubjectArea area) {
        return switch (area) {
            case ENGLISH -> ENGLISH_CUTS;
            case HISTORY -> HISTORY_CUTS;
            case KOREAN, MATH -> RELATIVE_100_CUTS;
            case EXPLORATION_1, EXPLORATION_2 -> RELATIVE_50_CUTS;
        };
    }
}
