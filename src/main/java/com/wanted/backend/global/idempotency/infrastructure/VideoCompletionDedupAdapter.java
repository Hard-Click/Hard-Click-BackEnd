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
     * <p>{@code ON DUPLICATE KEY UPDATE id = id}는 값 변화가 없어, 신규 삽입이면 affected=1, 이미 있으면
     * affected=0을 돌려준다(MySQL). 앱 락 없이 DB 유니크 제약만으로 최초 1회를 판별한다.
     * processed_at은 DB DEFAULT(CURRENT_TIMESTAMP)로 채운다.
     */
    @Override
    @Transactional
    public boolean claim(String consumerId, Long memberId, Long videoId) {
        int affected = entityManager.createNativeQuery("""
                INSERT INTO processed_video_completion (consumer_id, member_id, video_id)
                VALUES (:consumerId, :memberId, :videoId)
                ON DUPLICATE KEY UPDATE id = id
                """)
                .setParameter("consumerId", consumerId)
                .setParameter("memberId", memberId)
                .setParameter("videoId", videoId)
                .executeUpdate();
        return affected == 1;
    }
}
