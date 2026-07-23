package com.wanted.backend.global.idempotency.infrastructure;

import com.wanted.backend.global.idempotency.VideoCompletionDedup;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class VideoCompletionDedupAdapter implements VideoCompletionDedup {

    private final EntityManager entityManager;

    /**
     * (consumer_id, member_id, video_id) 유니크 키 위에서 원자적 선점.
     *
     * <p>{@code INSERT IGNORE}는 신규 삽입이면 affected=1, 유니크 키 충돌(이미 선점됨)이면 삽입이 무시돼
     * affected=0을 돌려준다 — 드라이버 설정과 무관하다. (참고: {@code ON DUPLICATE KEY UPDATE id=id}는
     * MySQL Connector/J 기본값 {@code useAffectedRows=false}(CLIENT_FOUND_ROWS)에서 매칭된 기존 행도
     * 1(found)로 보고해, 중복 재전달을 신규 선점으로 오판하므로 쓰지 않는다.)
     * 앱 락 없이 DB 유니크 제약만으로 최초 1회를 판별한다. processed_at은 DB DEFAULT(CURRENT_TIMESTAMP)로 채운다.
     */
    @Override
    @Transactional
    public boolean claim(String consumerId, Long memberId, Long videoId) {
        int affected = entityManager.createNativeQuery("""
                INSERT IGNORE INTO processed_video_completion (consumer_id, member_id, video_id)
                VALUES (:consumerId, :memberId, :videoId)
                """)
                .setParameter("consumerId", consumerId)
                .setParameter("memberId", memberId)
                .setParameter("videoId", videoId)
                .executeUpdate();
        return affected == 1;
    }
}
