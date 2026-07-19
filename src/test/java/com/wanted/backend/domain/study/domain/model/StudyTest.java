package com.wanted.backend.domain.study.domain.model;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyTest {

    private Study studyWith(int maxCount, int currentCount, StudyStatus status) {
        return Study.restore(45L, 1L, "수학 스터디", "MATH_1", "내용",
                maxCount, currentCount, status, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("정원이 차면 상태가 FULL(정원 마감)이 된다")
    void join_whenFull_becomesFull() {
        Study study = studyWith(3, 2, StudyStatus.ACTIVE);

        study.join();

        assertThat(study.getStatus()).isEqualTo(StudyStatus.FULL);
    }

    @Test
    @DisplayName("방장이 삭제(방폭)하면 상태가 DISSOLVED(해산)가 된다")
    void close_becomesDissolved() {
        Study study = studyWith(5, 1, StudyStatus.ACTIVE);

        study.close();

        assertThat(study.getStatus()).isEqualTo(StudyStatus.DISSOLVED);
    }

    @Test
    @DisplayName("정원 마감(FULL) 상태에서 자리가 나면 다시 ACTIVE로 재오픈된다")
    void leave_fromFull_reopensToActive() {
        Study study = studyWith(3, 3, StudyStatus.FULL);

        study.leave(2L);

        assertThat(study.getStatus()).isEqualTo(StudyStatus.ACTIVE);
    }

    @Test
    @DisplayName("해산(DISSOLVED)된 스터디는 강퇴가 일어나도 절대 재오픈되지 않는다")
    void kick_fromDissolved_neverReopens() {
        Study study = studyWith(5, 3, StudyStatus.DISSOLVED);

        study.kick();

        assertThat(study.getStatus()).isEqualTo(StudyStatus.DISSOLVED);
    }

    @Test
    @DisplayName("해산(DISSOLVED)된 스터디는 퇴장이 일어나도 절대 재오픈되지 않는다")
    void leave_fromDissolved_neverReopens() {
        Study study = studyWith(5, 3, StudyStatus.DISSOLVED);

        study.leave(2L);

        assertThat(study.getStatus()).isEqualTo(StudyStatus.DISSOLVED);
    }

    @Test
    @DisplayName("방장이 혼자 남은 방에서 퇴장하면 해산(DISSOLVED)이 된다")
    void leave_soloHost_becomesDissolved() {
        Study study = studyWith(5, 1, StudyStatus.ACTIVE);

        study.leave(1L);

        assertThat(study.getStatus()).isEqualTo(StudyStatus.DISSOLVED);
    }

    @Test
    @DisplayName("해산된 스터디에는 참여할 수 없다")
    void join_dissolved_throws() {
        Study study = studyWith(5, 1, StudyStatus.DISSOLVED);

        assertThatThrownBy(study::join)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_FULL.getMessage());
    }

    @Test
    @DisplayName("정원 마감된 스터디에는 참여할 수 없다")
    void join_full_throws() {
        Study study = studyWith(3, 3, StudyStatus.FULL);

        assertThatThrownBy(study::join)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_FULL.getMessage());
    }
}
