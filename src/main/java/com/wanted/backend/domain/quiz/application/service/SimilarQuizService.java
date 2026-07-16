package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.SimilarProblemRecommenderPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizResult;
import com.wanted.backend.domain.quiz.application.usecase.SimilarQuizUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 유사퀴즈 생성 서비스.
 *
 * 흐름: 강의의 모든 퀴즈를 모아 (1) 문항 맵/코스 제목 확보 → (2) 이 학생이 이 강의에서 틀린 문항 id 수집
 * → (3) 오답별로 추천기 호출해 유사 문항 id 확보 → (4) 코스 내 문항으로 조립.
 *
 * ⚠️ 현재는 **같은 코스 내 유사 문항만** 조립한다(추천 결과가 코스 밖 문항이면 스킵). 데모 시드는 단일 코스라
 * 문제 없으며, 교차-코스 조립은 문항 by-id 조회 추가와 함께 후속으로 확장한다.
 *
 * 트랜잭션 경계: 메서드에 `@Transactional`을 걸지 않는다. 각 조회(findAllByCourseId /
 * findByMemberIdAndQuizIdIn)는 도메인 객체(완전 매핑된 POJO)를 반환하므로 이후 접근에 지연로딩이
 * 없고, 느린 외부 추천 HTTP 호출을 DB 커넥션을 점유한 채 수행하지 않는다(커넥션 고갈 방지).
 */
@Service
@RequiredArgsConstructor
public class SimilarQuizService implements SimilarQuizUseCase {

    private static final int SIMILAR_PER_WRONG = 2;

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final SimilarProblemRecommenderPort recommender;

    @Override
    public SimilarQuizResult generateForCourse(Long memberId, Long courseId) {
        List<Quiz> quizzes = quizRepository.findAllByCourseId(courseId);
        if (quizzes.isEmpty()) {
            return null;
        }

        Map<Long, QuizQuestion> questionById = new LinkedHashMap<>();
        List<Long> quizIds = new ArrayList<>();
        for (Quiz quiz : quizzes) {
            quizIds.add(quiz.getId());
            for (QuizQuestion question : quiz.getQuestions()) {
                questionById.put(question.getId(), question);
            }
        }

        Set<Long> wrongQuestionIds = new LinkedHashSet<>();
        for (QuizSubmission submission : quizSubmissionRepository.findByMemberIdAndQuizIdIn(memberId, quizIds)) {
            submission.getAnswers().stream()
                    .filter(answer -> !answer.isCorrect())
                    .forEach(answer -> wrongQuestionIds.add(answer.getQuestionId()));
        }
        if (wrongQuestionIds.isEmpty()) {
            return null;
        }

        Set<Long> similarIds = new LinkedHashSet<>();
        for (Long wrongId : wrongQuestionIds) {
            for (Long similarId : recommender.recommendSimilar(memberId, wrongId, SIMILAR_PER_WRONG)) {
                if (similarId.equals(wrongId)) {
                    continue; // 원문제(자기 자신) 제외
                }
                if (wrongQuestionIds.contains(similarId)) {
                    continue; // 이미 틀린 문제 제외
                }
                if (questionById.containsKey(similarId)) {
                    similarIds.add(similarId); // 코스 내 문항만 조립
                }
            }
        }
        if (similarIds.isEmpty()) {
            return null;
        }

        List<SimilarQuizResult.Question> questions = new ArrayList<>();
        for (Long similarId : similarIds) {
            QuizQuestion question = questionById.get(similarId);
            List<String> options = question.getOptions().stream()
                    .map(option -> option.getOptionText())
                    .toList();
            questions.add(new SimilarQuizResult.Question(question.getId(), question.getQuestionText(), options));
        }

        String courseTitle = quizzes.get(0).getTitle();
        return new SimilarQuizResult(courseId, courseId, courseTitle + " 유사문제", questions);
    }
}
