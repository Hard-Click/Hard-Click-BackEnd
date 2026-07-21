package com.wanted.backend.domain.notice.infrastructure.persistence;

import com.wanted.backend.domain.notice.application.port.NoticeReadPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class NoticeReadAdapter implements NoticeReadPort {

    private final SpringDataNoticeReadRepository repository;
    private final Clock clock;

    public NoticeReadAdapter(SpringDataNoticeReadRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void markRead(Long memberId, Long noticeId) {
        // 대부분의 중복 호출은 exists 체크로 걸러진다. (트랜잭션 안에서 발생하는 유니크 충돌은
        // catch해도 세션이 rollback-only가 되어 무의미하므로, 드문 동시요청 경합은 유니크 제약에 맡긴다.)
        if (repository.existsByMemberIdAndNoticeId(memberId, noticeId)) {
            return;
        }
        repository.save(new NoticeReadJpaEntity(memberId, noticeId, LocalDateTime.now(clock)));
    }

    @Override
    public boolean isRead(Long memberId, Long noticeId) {
        if (memberId == null) {
            return false;
        }
        return repository.existsByMemberIdAndNoticeId(memberId, noticeId);
    }

    @Override
    public List<Long> findReadNoticeIds(Long memberId, List<Long> noticeIds) {
        if (memberId == null || noticeIds == null || noticeIds.isEmpty()) {
            return List.of();
        }
        return repository.findByMemberIdAndNoticeIdIn(memberId, noticeIds).stream()
                .map(NoticeReadJpaEntity::getNoticeId)
                .toList();
    }
}
