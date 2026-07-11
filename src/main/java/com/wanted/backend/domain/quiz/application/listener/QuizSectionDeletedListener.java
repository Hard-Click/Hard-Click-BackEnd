package com.wanted.backend.domain.quiz.application.listener;

import com.wanted.backend.domain.cource.domain.event.SectionDeletedEvent;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 강의 수정으로 섹션이 삭제되면(SectionDeletedEvent) 해당 섹션의 퀴즈를 soft-delete 한다.
 *
 * hard-delete하면 학생 제출 이력(quiz_submission)이 FK ON DELETE CASCADE로 함께 사라지므로,
 * 퀴즈를 deleted_at으로 soft-delete하여 이력을 보존한다(활성 조회에서만 제외).
 *
 * 동기 {@link EventListener}로 강의 수정과 '같은 트랜잭션'에서 실행되어 원자성을 보장한다.
 * (AFTER_COMMIT을 쓰면 커밋~삭제 사이에 orphan 퀴즈가 잠시 노출되거나, 리스너 실패 시 orphan이 잔존한다.)
 * cource는 quiz를 직접 참조하지 않고 이벤트로만 연결되어 도메인 결합을 낮춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSectionDeletedListener {

    private final QuizCommandUseCase quizCommandUseCase;

    @EventListener
    public void onSectionDeleted(SectionDeletedEvent event) {
        quizCommandUseCase.deleteBySectionIds(event.sectionIds());
    }
}
