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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 유사퀴즈 제출·채점 서비스.
 *
 * 유사퀴즈 문항은 기존 quiz_question 행을 참조하므로, 저장된 문항 id로 원문항만 직접 조회해 채점한다
 * (코스 전체 로딩 회피). 정답 보기 순서(answerIndex)는 optionNumber ASC로 정렬된 보기 목록에서 정답 보기의
 * 위치(0-based)다 — 생성(①) 응답이 노출한 보기 순서와 동일하므로 selectedIndex와 직접 비교할 수 있다.
 *
 * 트랜잭션 경계: 외부 호출이 없는 순수 조회 채점이므로 두 조회(findById·findQuestionsByIds)를 하나의
 * 읽기 전용 트랜잭션으로 묶어 Read Skew를 막는다.
 */
@Service
@RequiredArgsConstructor
public class SimilarQuizSubmissionService implements SubmitSimilarQuizUseCase {

    private final SimilarQuizRepository similarQuizRepository;
    private final QuizRepository quizRepository;
    private final SimilarQuizSubscriptionAccessPort subscriptionAccessPort;

    @Override
    @Transactional(readOnly = true)
    public SimilarQuizSubmissionResult submit(SubmitSimilarQuizCommand command) {
        if (!subscriptionAccessPort.hasActiveSubscription(command.memberId())) {
            throw new BusinessException(ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        }

        // 존재하지 않거나 본인 세트가 아니면 동일하게 404(존재 여부 노출 방지).
        SimilarQuiz similarQuiz = similarQuizRepository.findById(command.similarQuizId())
                .filter(sq -> sq.isOwnedBy(command.memberId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SIMILAR_QUIZ_NOT_FOUND));

        Map<Long, QuizQuestion> questionById = quizRepository.findQuestionsByIds(similarQuiz.getQuestionIds())
                .stream().collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

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
