package com.wanted.backend.domain.grass.application.query;

public record GetStudyTimeGrassQuery(
        Long memberId,
        Integer year,
        Integer month
) {
    public GetStudyTimeGrassQuery {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
    }

    // month 미지정(연간 전체) 호출용 편의 생성자 — 기존 호출부 하위호환.
    public GetStudyTimeGrassQuery(Long memberId, Integer year) {
        this(memberId, year, null);
    }
}
