package com.wanted.backend.domain.study.domain.model;

// 기존 CLOSED가 "정원 마감"과 "방장 해산" 두 의미로 겹쳐 있어 목록에서 해산만 골라
// 숨길 수 없었다(#586). FULL은 자리가 나면 ACTIVE로 재오픈될 수 있지만,
// DISSOLVED는 최종 상태로 절대 재오픈되지 않는다.
public enum StudyStatus {
    ACTIVE,
    FULL,
    DISSOLVED
}
