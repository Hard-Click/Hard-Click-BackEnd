package com.wanted.backend.domain.quiz.infrastructure.ai;

import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 복습 추천 AI(Python/FastAPI) 서버 호출 어댑터. {@link SimilarProblemRecommenderAdapter}와 동일한
 * {@code quiz.ai.*} 설정(base-url/enabled)을 공유하고 경로만 {@code review-path}로 다르다.
 * 계약: {@code GET {review-path}/{studentId}?k={}} → {@code {"reviews":[{"problem_id","section_id","similar":[...]}]}}.
 * {@code quiz.ai.enabled=false}면 빈 리스트 반환(기능 off), 호출 실패도 빈 리스트(fail-soft).
 */
@Slf4j
@Component
public class ReviewRecommenderAdapter implements ReviewRecommenderPort {

    private final RestClient restClient;
    private final String reviewPath;
    private final boolean enabled;

    public ReviewRecommenderAdapter(
            @Value("${quiz.ai.base-url}") String baseUrl,
            @Value("${quiz.ai.review-path}") String reviewPath,
            @Value("${quiz.ai.enabled}") boolean enabled
    ) {
        this.reviewPath = reviewPath;
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
    public List<ReviewItem> recommendReview(long studentId, int k) {
        if (!enabled) {
            log.info("복습 추천 비활성화(quiz.ai.enabled=false) — studentId={} 스킵", studentId);
            return List.of();
        }
        try {
            Map<?, ?> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(reviewPath + "/{studentId}")
                            .queryParam("k", k)
                            .build(studentId))
                    .retrieve()
                    .body(Map.class);
            if (body == null || !(body.get("reviews") instanceof List<?> reviews)) {
                return List.of();
            }
            List<ReviewItem> items = new ArrayList<>();
            for (Object entry : reviews) {
                if (!(entry instanceof Map<?, ?> item)) {
                    continue;
                }
                if (!(item.get("problem_id") instanceof Number problemId)) {
                    continue;
                }
                long sectionId = item.get("section_id") instanceof Number s ? s.longValue() : 0L;
                List<Long> similarIds = new ArrayList<>();
                if (item.get("similar") instanceof List<?> similar) {
                    for (Object sid : similar) {
                        if (sid instanceof Number n) {
                            similarIds.add(n.longValue());
                        }
                    }
                }
                items.add(new ReviewItem(problemId.longValue(), sectionId, similarIds));
            }
            return items;
        } catch (Exception e) {
            log.warn("복습 추천 호출 실패 — studentId={}, err={}", studentId, e.getMessage());
            return List.of();
        }
    }
}
