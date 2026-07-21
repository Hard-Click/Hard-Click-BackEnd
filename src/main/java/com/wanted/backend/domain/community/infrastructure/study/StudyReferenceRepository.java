package com.wanted.backend.domain.community.infrastructure.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyReferenceRepository extends JpaRepository<StudyReferenceEntity, Long> {

    // 해산(DISSOLVED)된 스터디는 전체 피드에서 제외한다(스터디 탭과 동일 기준, #586).
    List<StudyReferenceEntity> findByStatusNot(String status);
}
