package com.wanted.backend.domain.cource.domain.event;

import com.wanted.backend.global.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

// 강의 수정으로 섹션이 삭제될 때 발행 → 해당 섹션에 속한 퀴즈를 함께(cascade) 정리한다.
// quiz.section_id는 FK 없이 컬럼만 두므로(V3__quiz.sql), DB 제약 대신 도메인 이벤트로 정합성을 맞춘다.
public record SectionDeletedEvent(
        List<Long> sectionIds,
        Instant occurredAt
) implements DomainEvent {

    public static SectionDeletedEvent of(List<Long> sectionIds) {
        return new SectionDeletedEvent(List.copyOf(sectionIds), Instant.now());
    }
}
