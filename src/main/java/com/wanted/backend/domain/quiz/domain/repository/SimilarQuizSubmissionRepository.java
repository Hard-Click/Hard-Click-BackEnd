package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.SimilarQuizSubmission;

/**
 * 유사퀴즈(복습) 제출 이력 영속 포트.
 * 재응시를 허용하므로 저장마다 새 제출 행이 시간순으로 쌓인다(UNIQUE 없음).
 */
public interface SimilarQuizSubmissionRepository {

    SimilarQuizSubmission save(SimilarQuizSubmission submission);
}
