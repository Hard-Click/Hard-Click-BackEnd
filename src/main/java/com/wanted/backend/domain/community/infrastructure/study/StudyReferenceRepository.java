package com.wanted.backend.domain.community.infrastructure.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudyReferenceRepository extends JpaRepository<StudyReferenceEntity, Long> {

    // 전체 피드에 노출할 상태(ACTIVE/FULL)만 IN 으로 조회한다. NOT(DISSOLVED)은 인덱스 레인지 스캔을
    // 못 타 풀스캔이 되므로, 긍정형 IN 으로 idx_study_status_created 를 활용한다(스터디 탭과 동일 기준, #586).
    List<StudyReferenceEntity> findByStatusIn(Collection<String> statuses);
}
