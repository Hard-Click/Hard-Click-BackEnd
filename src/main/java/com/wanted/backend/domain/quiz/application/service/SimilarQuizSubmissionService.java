package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.usecase.SubmitSimilarQuizUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 유사퀴즈 제출·채점 서비스.
 *
 * 유사퀴즈 문항은 기존 quiz_question 행을 참조하므로, 저장된 문항 id 순서대로 원문항을 조회해 채점한다.
 * 정답 보기 순서(answerIndex)는 optionNumber ASC로 정렬된 보기 목록에서 정답 보기의 위치(0-based)다 —
 * 생성(①) 응답이 노출한 보기 순서와 동일하므로 selectedIndex와 직접 비교할 수 있다.
 *
 * 트랜잭션 경계: findById/findAllByCourseId 모두 완전 매핑 POJO를 반환하므로 서비스에 @Transactional을 걸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SimilarQuizSubmissionService implements SubmitSimilarQuizUseCase {

    private final SimilarQuizRepository similarQuizRepository;
    private final QuizRepository quizRepository;
    private final SimilarQuizSubscriptionAccessPort subscriptionAccessPort;

    @Override
    public SimilarQuizSubmissionResult submit(SubmitSimilarQuizCommand command) {
        if (!subscriptionAccessPort.hasActiveSubscription(command.memberId())) {
            throw new BusinessException(ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        }

        // 존재하지 않거나 본인 세트가 아니면 동일하게 404(존재 여부 노출 방지).
        SimilarQuiz similarQuiz = similarQuizRepository.findById(command.similarQuizId())
                .filter(sq -> sq.isOwnedBy(command.memberId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SIMILAR_QUIZ_NOT_FOUND));

        Map<Long, QuizQuestion> questionById = new LinkedHashMap<>();
        for (Quiz quiz : quizRepository.findAllByCourseId(similarQuiz.getCourseId())) {
            for (QuizQuestion question : quiz.getQuestions()) {
                questionById.put(question.getId(), question);
            }
        }

        Map<Long, Integer> selectedByQuestion = new HashMap<>();
        if (command.answers() != null) {
            for (SubmitSimilarQuizCommand.AnswerCommand answer : command.answers()) {
                selectedByQuestion.put(answer.questionId(), answer.selectedIndex());
            }
        }

        List<SimilarQuizSubmissionResult.Question> questions = new ArrayList<>();
        int correctCount = 0;
        for (Long questionId : similarQuiz.getQuestionIds()) {
            QuizQuestion question = questionById.get(questionId);
            if (question == null) {
                continue; // 생성 이후 원문항이 소프트 삭제된 경우 방어 — 채점 대상에서 제외.
            }
            List<QuizOption> options = question.getOptions();
            List<String> optionTexts = options.stream().map(QuizOption::getOptionText).toList();
            int answerIndex = correctIndexOf(options);
            Integer selectedIndex = selectedByQuestion.get(questionId);
            boolean correct = selectedIndex != null && selectedIndex == answerIndex;
            if (correct) {
                correctCount++;
            }
            questions.add(new SimilarQuizSubmissionResult.Question(
                    questionId, question.getQuestionText(), optionTexts,
                    answerIndex, selectedIndex, question.getExplanation(), correct));
        }

        int totalCount = questions.size();
        int score = totalCount == 0 ? 0 : Math.round((float) correctCount * 100 / totalCount);

        return new SimilarQuizSubmissionResult(similarQuiz.getId(), score, correctCount, totalCount, questions);
    }

    // optionNumber ASC 정렬 목록에서 정답 보기의 위치(0-based). 정답 미표기(비정상 데이터)면 -1.
    private int correctIndexOf(List<QuizOption> options) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isCorrect()) {
                return i;
            }
        }
        return -1;
    }
}
