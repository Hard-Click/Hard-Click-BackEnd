package com.wanted.backend.domain.study.application;

import com.wanted.backend.domain.study.application.command.JoinStudyCommand;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정원 마지막 한 자리를 두고 여러 스레드가 동시에 참여를 시도했을 때, DB 비관적 락(findByIdForUpdate)이
 * 정확히 1명만 통과시키고 나머지는 STUDY_FULL로 실패하는지 실제 트랜잭션/DB로 검증한다.
 * 순수 Mockito 단위 테스트로는 실제 락 경합을 재현할 수 없어 전체 스프링 컨텍스트 + 실제 DB로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudyJoinConcurrencyTest {

    private static final long HOST_ID = 999_990_000L;
    private static final int THREAD_COUNT = 10;

    @Autowired
    private StudyCommandUseCase studyCommandUseCase;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private ChatRoomCommandPort chatRoomCommandPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long studyId;
    private Long chatRoomId;

    @BeforeEach
    void setUp() {
        Study study = Study.create(HOST_ID, "동시성 테스트 스터디", "MATH_1", THREAD_COUNT, "내용");
        Study saved = studyRepository.save(study);
        studyId = saved.getId();

        // 정원(THREAD_COUNT) - 1명만 남기고 미리 채워 "마지막 한 자리"를 만든다.
        Study almostFull = Study.restore(studyId, HOST_ID, saved.getTitle(), saved.getSubject(), saved.getContent(),
                saved.getMaxCount(), THREAD_COUNT - 1, StudyStatus.ACTIVE, saved.getCreatedAt(), saved.getUpdatedAt());
        studyRepository.save(almostFull);

        chatRoomId = chatRoomCommandPort.createRoom(studyId, HOST_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM chat_room_participant WHERE chat_room_id = ?", chatRoomId);
        jdbcTemplate.update("DELETE FROM chat_message WHERE chat_room_id = ?", chatRoomId);
        jdbcTemplate.update("DELETE FROM chat_room WHERE chat_room_id = ?", chatRoomId);
        jdbcTemplate.update("DELETE FROM study_participant WHERE study_id = ?", studyId);
        jdbcTemplate.update("DELETE FROM study WHERE study_id = ?", studyId);
    }

    @Test
    @DisplayName("정원 1자리를 두고 N개 스레드가 동시에 참여해도 정확히 1명만 성공하고 나머지는 STUDY_FULL로 실패한다")
    void onlyOneSucceeds_whenMultipleThreadsRaceForLastSlot() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            long memberId = HOST_ID + 1 + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    studyCommandUseCase.join(new JoinStudyCommand(studyId, memberId));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.STUDY_FULL) {
                        fullCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(e);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(fullCount.get()).isEqualTo(THREAD_COUNT - 1);

        Study finalStudy = studyRepository.findById(studyId).orElseThrow();
        assertThat(finalStudy.getCurrentCount()).isEqualTo(THREAD_COUNT);
        assertThat(finalStudy.getStatus()).isEqualTo(StudyStatus.FULL);

        Integer participantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM study_participant WHERE study_id = ?", Integer.class, studyId);
        assertThat(participantCount).isEqualTo(1);
    }
}
