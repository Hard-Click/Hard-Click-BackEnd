package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort;
import com.wanted.backend.domain.quiz.application.port.ReviewRecommenderPort.ReviewItem;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult.Question;
import com.wanted.backend.domain.quiz.application.result.ReviewQuizResult.ReviewGroup;
import com.wanted.backend.domain.quiz.application.usecase.ReviewQuizUseCase;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
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
 * → (3) 원문제별로 묶고, 각 그룹을 그 코스의 {@code SimilarQuiz}로 저장(제출용 id 확보) → 급한 순 결과 조립.
 * 인덱싱 전/조회 불가 문항은 스킵한다. 유사문제는 원문제와 같은 코스(추천기 course 격리)라 그룹당 코스가 하나다.
 *
 * 트랜잭션 경계: {@code @Transactional}을 걸지 않는다 — 조회는 완전 매핑 POJO를 반환해 지연로딩이
 * 없고, 느린 외부 추천 HTTP 호출을 DB 커넥션을 점유한 채 수행하지 않는다(커넥션 고갈 방지).
 * 저장은 HTTP·조회가 끝난 뒤 그룹별로 짧게 수행한다. 코스 미상 그룹은 저장을 건너뛰고 응시만(제출 불가) 노출한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewQuizService implements ReviewQuizUseCase {

    private static final int SIMILAR_PER_ORIGINAL = 2;
    private static final String REVIEW_TITLE = "복습 유사문제";

    private final ReviewRecommenderPort recommender;
    private final QuizRepository quizRepository;
    private final SimilarQuizRepository similarQuizRepository;
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
            List<Long> questionIds = new ArrayList<>();
            for (Long similarId : item.similarIds().stream().distinct().toList()) {
                QuizQuestion question = questionById.get(similarId);
                if (question == null) {
                    continue; // 인덱싱 전/조회 불가 문항 스킵
                }
                List<String> options = question.getOptions().stream()
                        .map(option -> option.getOptionText())
                        .toList();
                questions.add(new Question(question.getId(), question.getQuestionText(), options));
                questionIds.add(question.getId());
            }
            if (questions.isEmpty()) {
                continue;
            }
            Long similarQuizId = persistGroup(memberId, item.courseId(), questionIds);
            groups.add(new ReviewGroup(similarQuizId, item.problemId(), item.sectionId(), questions));
        }

        return groups.isEmpty() ? null : new ReviewQuizResult(groups);
    }

    /**
     * 그룹을 그 코스의 SimilarQuiz로 저장하고 제출용 id를 돌려준다.
     * 코스 미상(courseId≤0)이거나, 추천기가 준 courseId를 DB 기준으로 검증했을 때 문항 중
     * 하나라도 그 코스 소속이 아니면(오래된/잘못된 추천 결과) 저장을 건너뛰고 null(응시만, 제출 불가)
     * — 부분 데이터가 전체를 막지 않게 하면서도, 다른 코스 문항이 잘못된 코스로 영속되지 않게 한다.
     */
    private Long persistGroup(Long memberId, long courseId, List<Long> questionIds) {
        if (courseId <= 0) {
            return null;
        }
        List<Long> verifiedIds = quizRepository.findQuestionIdsBelongingToCourse(questionIds, courseId);
        if (verifiedIds.size() != questionIds.size()) {
            return null;
        }
        SimilarQuiz saved = similarQuizRepository.save(
                SimilarQuiz.create(memberId, courseId, null, REVIEW_TITLE, questionIds));
        return saved.getId();
    }
}
