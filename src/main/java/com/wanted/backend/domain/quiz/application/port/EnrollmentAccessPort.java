package com.wanted.backend.domain.quiz.application.port;

/**
 * 회원이 특정 강의를 유효하게 수강 중인지 확인하는 아웃바운드 포트.
 * 내 퀴즈 목록 조회 시 미수강 강의의 퀴즈 노출을 차단하는 데 사용한다.
 */
public interface EnrollmentAccessPort {

    boolean hasActiveEnrollment(Long memberId, Long courseId);
}
