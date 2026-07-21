package com.wanted.backend.domain.study_timer.application.service;

import com.wanted.backend.domain.study_timer.application.query.GetStudyTimerSessionsByDateQuery;
import com.wanted.backend.domain.study_timer.application.usecase.GetStudyTimerSessionsByDateUseCase;
import com.wanted.backend.domain.study_timer.domain.repository.StudyTimerSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStudyTimerSessionsByDateService implements GetStudyTimerSessionsByDateUseCase {

    private final StudyTimerSessionRepository studyTimerSessionRepository;

    @Override
    public List<StudyTimerSessionView> handle(GetStudyTimerSessionsByDateQuery query) {
        return studyTimerSessionRepository.findEndedSessionsByDate(query.memberId(), query.date()).stream()
                .map(session -> new StudyTimerSessionView(
                        session.id(),
                        session.startedAt(),
                        session.endedAt()))
                .toList();
    }
}
