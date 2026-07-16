package com.wanted.backend.domain.student_onboarding.domain.policy;

import com.wanted.backend.domain.student_onboarding.domain.model.WeeklyAvailability;

/**
 * 주간 가용시간 -> 하루 학습 상한(student_capacity.daily_cap_min) 유도.
 *
 * <h2>가용시간에서 유도하는 건 팀 결정이다 (되돌리지 마)</h2>
 * 화면에 하루 상한 입력을 추가하는 대신 가용시간에서 유도하기로 결정했다(2026-07-15).
 * "온보딩 화면이 안 물어보니 유도는 임시방편"이라고 보고 입력 화면을 추가하는 리팩터를 하지 말 것.
 *
 * <p>강사가 강의등록 때 넣는 코스별 강도 상한(course.daily_max_minutes, 기본 120분)과는
 * <b>다른 값이다</b>. 저건 "이 코스를 하루 몇 분까지", 이건 "이 학생이 하루 총 몇 분까지"다.
 * 코스 상한만 쓰면 다중 코스 수강 시 cap 이 코스 수만큼 뚫린다(V3.1.7 이 학생 단위로 옮긴 이유).
 * CP-SAT 은 둘을 함께 봐야 한다 — 하루 배치량 = min(학생 총량, 코스별 강도 상한).
 *
 * <h2>⚠️ 상·하한 상수는 아직 미확정이다</h2>
 * 유도 방식은 확정됐지만 아래 MAX/MIN 값 자체는 기획 확정 전 placeholder다.
 * CP-SAT 이 이 값을 하드 상한으로 쓰므로, 과대 추정하면 학생이 못 따라가는 계획이 나오고
 * 과소 추정하면 수능일까지 진도가 안 나온다.
 */
public final class DailyCapPolicy {

    /**
     * 하루 학습 상한의 정책 최대치(분). 가용시간이 아무리 많아도 이 값을 넘기지 않는다.
     * Python 스케줄러의 콜드스타트 폴백(주 420분 = 하루 60분 x 7)과 달리 이건 '상한'이다.
     * ⚠️ placeholder - 기획 확정 필요.
     */
    public static final int MAX_DAILY_CAP_MINUTES = 480;

    /**
     * 하루 학습 상한의 정책 최소치(분). 가용시간을 극단적으로 적게 넣어도 계획이 아예 못 서는 걸 막는다.
     * ⚠️ placeholder - 기획 확정 필요.
     */
    public static final int MIN_DAILY_CAP_MINUTES = 30;

    private DailyCapPolicy() {
    }

    /**
     * 학습일 평균 가용 분을 정책 상·하한으로 클램프해 하루 상한을 만든다.
     *
     * @return 하루 학습 상한(분). 쉬는 날을 제외한 학습일이 하나도 없으면 0.
     */
    public static int deriveDailyCapMinutes(WeeklyAvailability availability) {
        int average = availability.averageAvailableMinutesPerStudyDay();
        if (average <= 0) {
            return 0;
        }
        return Math.max(MIN_DAILY_CAP_MINUTES, Math.min(MAX_DAILY_CAP_MINUTES, average));
    }
}
