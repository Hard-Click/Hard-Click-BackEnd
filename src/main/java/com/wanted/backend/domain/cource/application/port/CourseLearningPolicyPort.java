package com.wanted.backend.domain.cource.application.port;

import java.util.Optional;

/**
 * 강의 학습 정책(course_learning_policy) 아웃바운드 포트.
 * CP-SAT 스케줄러(Python-Server)가 읽는 코스별 권장 완강 주수 / 하루 학습 상한을 기록·조회한다.
 * (기존에 course_learning_policy 테이블은 있었으나 write 경로가 없어 강의 등록 시 비어 있었음 — 이 포트로 보강)
 */
public interface CourseLearningPolicyPort {

    /**
     * @param courseId          대상 강의 ID
     * @param recommendedWeeks  권장 완강 기간(주), nullable
     * @param dailyMaxMinutes   하루 학습 상한(분), nullable → 호출부에서 기본값 적용
     */
    void save(Long courseId, Integer recommendedWeeks, Integer dailyMaxMinutes);

    /**
     * 강의 수정 화면 프리필용 조회. 정책 레코드가 없으면(구 강의) 빈 Optional.
     */
    Optional<LearningPolicy> find(Long courseId);

    /** 저장된 학습 정책 값. 두 필드 모두 nullable. */
    record LearningPolicy(Integer recommendedWeeks, Integer dailyMaxMinutes) {
    }
}
