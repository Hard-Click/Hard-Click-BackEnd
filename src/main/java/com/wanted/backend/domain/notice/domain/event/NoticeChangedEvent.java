package com.wanted.backend.domain.notice.domain.event;

import com.wanted.backend.global.domain.DomainEvent;

import java.time.Instant;

/**
 * 전체 공지 상태가 변경(전체 공지 생성 / 공지 수정 / 공지 삭제)됐을 때 발행되는 이벤트.
 *
 * 관리자 대시보드의 recentNotices/totalNoticeCount(GLOBAL·PUBLISHED 기준)에 영향을 주므로,
 * admin_dashboard 도메인이 이 이벤트를 구독해 캐시를 무효화한다. 공지 도메인은 대시보드 캐시의
 * 존재를 알 필요가 없도록 이벤트로만 연결한다.
 *
 * 강의(COURSE) 공지 생성은 대시보드 집계 대상이 아니라 이 이벤트를 발행하지 않는다.
 */
public record NoticeChangedEvent(
        Long noticeId,
        ChangeType changeType,
        Instant occurredAt
) implements DomainEvent {

    public enum ChangeType { CREATED_GLOBAL, UPDATED, DELETED }

    public static NoticeChangedEvent of(Long noticeId, ChangeType changeType) {
        return new NoticeChangedEvent(noticeId, changeType, Instant.now());
    }
}
