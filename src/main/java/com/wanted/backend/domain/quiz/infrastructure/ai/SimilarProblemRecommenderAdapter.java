package com.wanted.backend.domain.quiz.infrastructure.ai;

import com.wanted.backend.domain.quiz.application.port.SimilarProblemRecommenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 유사문제 추천 AI(Python/FastAPI) 서버 호출 어댑터. QuizSubmissionAiAdapter와 동일한 {@code quiz.ai.*} 설정을 공유한다.
 * 추천기 계약: {@code GET {similar-path}/{problemId}?student_id={}&k={}} → {@code {"problems":[원문제, 유사...]}}.
 * {@code quiz.ai.enabled=false}면 빈 리스트 반환(기능 off) — 추천 서버 확정/배포 전까지 안전.
 */
@Slf4j
@Component
public class SimilarProblemRecommenderAdapter implements SimilarProblemRecommenderPort {

    private final RestClient restClient;
    private final String similarPath;
    private final boolean enabled;

    public SimilarProblemRecommenderAdapter(
            @Value("${quiz.ai.base-url}") String baseUrl,
            @Value("${quiz.ai.similar-path}") String similarPath,
            @Value("${quiz.ai.enabled}") boolean enabled
    ) {
        this.similarPath = similarPath;
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
    public List<Long> recommendSimilar(long studentId, long problemId, int k) {
        if (!enabled) {
            log.info("유사문제 추천 비활성화(quiz.ai.enabled=false) — problemId={} 스킵", problemId);
            return List.of();
        }
        try {
            Map<?, ?> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(similarPath + "/{problemId}")
                            .queryParam("student_id", studentId)
                            .queryParam("k", k)
                            .build(problemId))
                    .retrieve()
                    .body(Map.class);
            if (body == null || !(body.get("problems") instanceof List<?> problems)) {
                return List.of();
            }
            return problems.stream()
                    .filter(Number.class::isInstance)
                    .map(v -> ((Number) v).longValue())
                    .toList();
        } catch (Exception e) {
            log.warn("유사문제 추천 호출 실패 — problemId={}, err={}", problemId, e.getMessage());
            return List.of();
        }
    }
}
