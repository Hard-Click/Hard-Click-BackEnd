package com.wanted.backend.domain.enrollment_management.infrastructure.ai;

import com.wanted.backend.domain.enrollment_management.application.port.ScheduleGenerationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * AI 스케줄러(Python/Flask) 즉시 생성 호출 어댑터.
 * 계약: {@code POST {generate-path}} body {@code {"member_id": ...}} — Python이 해당 회원의
 * 활성 enrollment·가용시간을 직접 조회해 주간 스케줄을 생성·저장한다(주간 배치와 동일 로직).
 * {@code schedule.ai.enabled=false}면 스킵(기능 off) — 이 경우 스케줄은 주간 배치 때 생성된다.
 */
@Slf4j
@Component
public class ScheduleGenerationAiAdapter implements ScheduleGenerationPort {

    private final RestClient restClient;
    private final String generatePath;
    private final boolean enabled;

    public ScheduleGenerationAiAdapter(
            @Value("${schedule.ai.base-url}") String baseUrl,
            @Value("${schedule.ai.generate-path}") String generatePath,
            @Value("${schedule.ai.enabled}") boolean enabled
    ) {
        this.generatePath = generatePath;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        // CP-SAT 최적화가 수 초 걸릴 수 있다 — 전용 비동기 풀(schedulerAiExecutor)에서만 블로킹되므로 넉넉히.
        requestFactory.setReadTimeout(60000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void requestGeneration(Long memberId) {
        if (!enabled) {
            log.info("스케줄 즉시 생성 비활성화(schedule.ai.enabled=false) — memberId={} 스킵", memberId);
            return;
        }
        Map<?, ?> body = restClient.post()
                .uri(generatePath)
                .body(Map.of("member_id", memberId))
                .retrieve()
                .body(Map.class);
        log.info("스케줄 즉시 생성 완료 — memberId={}, result={}", memberId, body);
    }
}
