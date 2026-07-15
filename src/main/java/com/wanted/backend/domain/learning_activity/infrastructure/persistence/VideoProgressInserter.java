package com.wanted.backend.domain.learning_activity.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VideoProgressInserter {

    private final SpringDataVideoProgressRepository repository;

    /**
     * 진도 행을 별도 트랜잭션에서 INSERT한다.
     *
     * <p>동시 요청이 같은 (member_id, video_id)를 INSERT하면 한쪽은 uk_video_progress_member_video에
     * 걸린다. 이때 바깥 트랜잭션에서 그대로 터뜨리면 트랜잭션이 rollback-only로 오염돼 경합에 밀린
     * 요청이 기존 행을 읽어 갱신하는 복구를 할 수 없다. REQUIRES_NEW로 분리해 실패를 이 트랜잭션
     * 안에 가둔다.
     *
     * <p>제약 위반을 호출부에서 잡을 수 있도록 saveAndFlush로 즉시 flush한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoProgressJpaEntity insert(VideoProgressJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }
}
