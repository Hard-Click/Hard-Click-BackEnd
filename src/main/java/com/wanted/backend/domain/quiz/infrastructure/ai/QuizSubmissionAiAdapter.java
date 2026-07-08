package com.wanted.backend.domain.quiz.infrastructure.ai;

import com.wanted.backend.domain.quiz.application.port.QuizSubmissionAiPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 채점된 퀴즈 제출 내용을 AI(Python/FastAPI) 서버로 전송하는 어댑터.
 * 결제 PG 연동(TossPaymentClient)과 동일한 RestClient 구성을 따른다.
 *
 * AI 서버 API 스펙이 확정되기 전까지는 {@code quiz.ai.enabled=false}로 두어 실제 전송을 건너뛴다.
 * 스펙 확정 시 enabled=true + submission-path/payload 형식만 맞추면 된다.
 */
@Slf4j
@Component
public class QuizSubmissionAiAdapter implements QuizSubmissionAiPort {

    private final RestClient restClient;
    private final String submissionPath;
    private final boolean enabled;

    public QuizSubmissionAiAdapter(
            @Value("${quiz.ai.base-url}") String baseUrl,
            @Value("${quiz.ai.submission-path}") String submissionPath,
            @Value("${quiz.ai.enabled}") boolean enabled
    ) {
        this.submissionPath = submissionPath;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void sendSubmission(SubmissionPayload payload) {
        if (!enabled) {
            log.info("퀴즈 AI 전송 비활성화(quiz.ai.enabled=false) — submissionId={} 전송 스킵",
                    payload.submissionId());
            return;
        }

        restClient.post()
                .uri(submissionPath)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("퀴즈 제출 AI 전송 완료 — submissionId={}", payload.submissionId());
    }
}
