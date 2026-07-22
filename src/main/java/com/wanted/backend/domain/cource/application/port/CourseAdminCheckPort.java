package com.wanted.backend.domain.cource.application.port;

/**
 * 요청자가 관리자(ROLE_ADMIN)인지 확인하는 포트.
 * 관리자는 소유 강사가 아니어도 모든 강의를 등록·수정·삭제·공개전환할 수 있다.
 */
public interface CourseAdminCheckPort {

    boolean isAdmin(Long memberId);
}
