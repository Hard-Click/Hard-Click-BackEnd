package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.SubmitSimilarQuizCommand;
import com.wanted.backend.domain.quiz.application.port.EnrollmentAccessPort;
import com.wanted.backend.domain.quiz.application.port.ReviewCompletionPort;
import com.wanted.backend.domain.quiz.application.port.SimilarQuizSubscriptionAccessPort;
import com.wanted.backend.domain.quiz.application.result.SimilarQuizSubmissionResult;
import com.wanted.backend.domain.quiz.application.usecase.SubmitSimilarQuizUseCase;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.model.SimilarQuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
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
 * 트랜잭션 경계: 채점 결과(제출·답안)를 저장하므로 쓰기 트랜잭션이다. 조회(findById·findQuestionsByIds)와
 * 저장을 하나의 트랜잭션으로 묶는다. 재응시를 허용하므로 저장마다 새 제출 이력이 시간순으로 쌓인다
 * (추천기가 라운드 단위로 난이도·시간 신호를 판정하는 근거).
 */
@Service
@RequiredArgsConstructor
public class SimilarQuizSubmissionService implements SubmitSimilarQuizUseCase {

    private final SimilarQuizRepository similarQuizRepository;
    private final QuizRepository quizRepository;
    private final SimilarQuizSubmissionRepository submissionRepository;
    private final SimilarQuizSubscriptionAccessPort subscriptionAccessPort;
    private final EnrollmentAccessPort enrollmentAccessPort;
    private final ReviewCompletionPort reviewCompletionPort;
    // 제출 시각은 JVM 기본 타임존(컨테이너 UTC)이 아니라 팀 표준 Clock(Asia/Seoul)으로 계산한다.
    // 복습 완료 전진의 '오늘'이 스케줄 조회(ScheduleController)의 KST '오늘'과 어긋나지 않게 하기 위함.
    private final Clock clock;

    @Override
    @Transactional
    public SimilarQuizSubmissionResult submit(SubmitSimilarQuizCommand command) {
        if (!subscriptionAccessPort.hasActiveSubscription(command.memberId())) {
            throw new BusinessException(ErrorCode.SIMILAR_QUIZ_SUBSCRIPTION_REQUIRED);
        }

        // 존재하지 않거나 본인 세트가 아니면 동일하게 404(존재 여부 노출 방지).
        SimilarQuiz similarQuiz = similarQuizRepository.findById(command.similarQuizId())
                .filter(sq -> sq.isOwnedBy(command.memberId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SIMILAR_QUIZ_NOT_FOUND));

        // 수강 만료 후 과거 세트로 제출해 review_card 를 전진시키는 우회를 막는다.
        // 일반 퀴즈 응시/조회(QuizQueryService)와 동일하게 활성 수강을 요구한다.
        if (!enrollmentAccessPort.hasActiveEnrollment(command.memberId(), similarQuiz.getCourseId())) {
            throw new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
        }

        Map<Long, QuizQuestion> questionById = quizRepository.findQuestionsByIds(similarQuiz.getQuestionIds())
                .stream().collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

        Map<Long, Integer> selectedByQuestion = new HashMap<>();
        Map<Long, Integer> timeByQuestion = new HashMap<>();
        if (command.answers() != null) {
            for (SubmitSimilarQuizCommand.AnswerCommand answer : command.answers()) {
                selectedByQuestion.put(answer.questionId(), answer.selectedIndex());
                timeByQuestion.put(answer.questionId(), answer.timeSpentSeconds());
            }
        }

        List<SimilarQuizSubmissionResult.Question> questions = new ArrayList<>();
        List<SimilarQuizSubmission.Answer> answersToPersist = new ArrayList<>();
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
            answersToPersist.add(SimilarQuizSubmission.Answer.of(
                    questionId, selectedIndex, correct, timeByQuestion.get(questionId)));
        }

        int totalCount = questions.size();
        int score = totalCount == 0 ? 0 : Math.round((float) correctCount * 100 / totalCount);

        LocalDateTime submittedAt = LocalDateTime.now(clock);

        // 복습 이력 저장(재응시 허용 → 시간순 누적). 추천기가 이 이력으로 난이도·시간 신호를 판정한다.
        submissionRepository.save(SimilarQuizSubmission.create(
                similarQuiz.getId(), command.memberId(), score, totalCount, correctCount,
                submittedAt, answersToPersist));

        // 제출 = 그 코스의 '오늘 복습 완료' → 오늘 due 복습 카드를 다음 주기로 전진(캘린더에서 빠짐).
        // 같은 트랜잭션에서 동기 처리해야 제출 응답 시점에 캘린더가 이미 최신이다(FE revalidate 직후 페치 레이스 방지).
        // 오늘 due 카드가 없으면 no-op — 완료 대상 없음은 정상이므로 예외로 다루지 않는다.
        reviewCompletionPort.completeTodayReviews(command.memberId(), similarQuiz.getCourseId(), submittedAt);

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
