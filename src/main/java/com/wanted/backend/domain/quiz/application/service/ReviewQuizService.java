package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort;
import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort.ReviewItem;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult.Question;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult.ReviewGroup;
import com.wanted.backend.domain.quiz.application.usecase.ReviewQuizUseCase;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 복습 추천 생성 서비스('안 B'): 원문제 선정을 추천기(Python)에 위임하고, 그 결과의 유사문제를
 * 코스 경계 없이(findQuestionsByIds) 조립해 응시용 세트를 만든다.
 *
 * 흐름: (0) 구독 게이트 → (1) 추천기 호출(원문제+유사 id) → (2) 유사 문항을 by-id 일괄 조회
 * → (3) 원문제별로 묶어 급한 순 결과 조립. 인덱싱 전/조회 불가 문항은 스킵한다.
 *
 * 트랜잭션 경계: {@code @Transactional}을 걸지 않는다 — 조회는 완전 매핑 POJO를 반환해 지연로딩이
 * 없고, 느린 외부 추천 HTTP 호출을 DB 커넥션을 점유한 채 수행하지 않는다(커넥션 고갈 방지).
 * 영속/제출은 후속: student-global 세트라 {@code SimilarQuiz}(courseId 필수)로 바로 저장할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class ReviewQuizService implements ReviewQuizUseCase {

    private static final int SIMILAR_PER_ORIGINAL = 2;

    private final ReviewRecommenderPort recommender;
    private final QuizRepository quizRepository;
    private final SimilarQuizSubscriptionAccessPort subscriptionAccessPort;

    @Override
    public ReviewQuizResult generateForStudent(Long memberId) {
        if (!subscriptionAccessPort.hasActiveSubscription(memberId)) {
            throw new BusinessException(ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        }

        List<ReviewItem> items = recommender.recommendReview(memberId, SIMILAR_PER_ORIGINAL);
        if (items.isEmpty()) {
            return null; // 추천 근거(이력) 없음 또는 추천 서버 off
        }

        List<Long> similarIds = items.stream()
                .flatMap(item -> item.similarIds().stream())
                .distinct()
                .toList();
        if (similarIds.isEmpty()) {
            return null;
        }

        Map<Long, QuizQuestion> questionById = new LinkedHashMap<>();
        for (QuizQuestion question : quizRepository.findQuestionsByIds(similarIds)) {
            questionById.put(question.getId(), question);
        }

        List<ReviewGroup> groups = new ArrayList<>();
        for (ReviewItem item : items) {
            List<Question> questions = new ArrayList<>();
            for (Long similarId : item.similarIds()) {
                QuizQuestion question = questionById.get(similarId);
                if (question == null) {
                    continue; // 인덱싱 전/조회 불가 문항 스킵
                }
                List<String> options = question.getOptions().stream()
                        .map(option -> option.getOptionText())
                        .toList();
                questions.add(new Question(question.getId(), question.getQuestionText(), options));
            }
            if (!questions.isEmpty()) {
                groups.add(new ReviewGroup(item.problemId(), item.sectionId(), questions));
            }
        }

        return groups.isEmpty() ? null : new ReviewQuizResult(groups);
    }
}
