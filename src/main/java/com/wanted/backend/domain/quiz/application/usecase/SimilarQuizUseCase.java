package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;

public interface SimilarQuizUseCase {

    /**
     * 강의(course) 오답 기반 유사문제 세트를 생성·영속한다. 구독 회원만 이용 가능.
     * week는 진입(캘린더) 주차 — 캘린더 확정 전까지 선택 값이며 제목/메타에만 반영한다.
     * 오답이 없거나 유사문제가 없으면 null(정상 empty)을 반환한다.
     */
    SimilarQuizResult generateForCourse(Long memberId, Long courseId, Integer week);
}
