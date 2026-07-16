package com.wanted.backend.domain.study_schedule.domain.model;

/**
 * 학생 할 일의 상태. student_todo.status ENUM('PLANNED','DONE') 과 1:1.
 *
 * <p>schedule_slot.status 에는 MISSED 가 있지만 여기엔 없다 - 미달성 판정은 AI 계획에만 적용된다.
 *
 * <p>String 이 아니라 enum 으로 두는 이유: MySQL ENUM 컬럼은 {@code @Enumerated(EnumType.STRING)} +
 * Java enum 으로 매핑해야 Hibernate validate 를 통과한다(String 필드면 varchar 를 기대해서 드리프트로 잡힌다).
 * members.role/status 도 같은 패턴이다.
 */
public enum TodoStatus {

    /** 아직 안 함 - 생성 시 기본값 */
    PLANNED,

    /** 완료 체크됨 - 오늘 진행률에 반영된다 */
    DONE
}
