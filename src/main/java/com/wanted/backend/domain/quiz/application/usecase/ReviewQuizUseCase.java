package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult;

/**
 * 복습 추천 유스케이스 — 학생 id만으로 '무엇을 복습할지'(원문제)+유사문제 세트를 생성한다('안 B').
 * 구독 회원 전용. 추천 근거(이력)가 없거나 조립할 문항이 없으면 null(정상 empty).
 */
public interface ReviewQuizUseCase {

    ReviewQuizResult generateForStudent(Long memberId);
}
