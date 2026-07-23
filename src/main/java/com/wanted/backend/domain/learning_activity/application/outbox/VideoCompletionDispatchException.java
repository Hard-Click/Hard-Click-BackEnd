package com.wanted.backend.domain.learning_activity.application.outbox;

import java.util.List;

/**
 * outbox 전달 중 하나 이상의 소비자가 실패했음을 알려 relay 재시도를 유발하는 예외.
 */
public class VideoCompletionDispatchException extends RuntimeException {

    public VideoCompletionDispatchException(List<String> failedConsumerIds) {
        super("소비자 처리 실패: " + String.join(", ", failedConsumerIds));
    }
}
