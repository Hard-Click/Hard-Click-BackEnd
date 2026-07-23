package com.wanted.backend.domain.learning_activity.infrastructure;

import com.wanted.backend.domain.learning_activity.application.port.CourseProgressQueryPort;
import com.wanted.backend.domain.learning_activity.application.port.VideoPlayUrlPort;
import com.wanted.backend.domain.learning_activity.domain.model.VideoAccessInfo;
import com.wanted.backend.domain.learning_activity.domain.model.VideoProgress;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.CatalogCourseReferenceEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.CatalogCourseReferenceRepository;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.CourseSectionReferenceEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.CourseSectionReferenceRepository;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.LessonReferenceEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.LessonReferenceRepository;
import com.wanted.backend.domain.learning_activity.infrastructure.catalog.VideoCatalogAdapter;
import com.wanted.backend.domain.learning_activity.infrastructure.enrollment.EnrollmentAccessAdapter;
import com.wanted.backend.domain.learning_activity.infrastructure.enrollment.EnrollmentReferenceJpaEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.enrollment.SpringDataEnrollmentAccessRepository;
import com.wanted.backend.domain.learning_activity.infrastructure.persistence.CourseProgressQueryAdapter;
import com.wanted.backend.domain.learning_activity.infrastructure.persistence.VideoProgressInserter;
import com.wanted.backend.domain.learning_activity.infrastructure.persistence.VideoProgressRepositoryAdapter;
import com.wanted.backend.domain.learning_activity.infrastructure.persistence.VideoProgressJpaEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.persistence.SpringDataVideoProgressRepository;
import com.wanted.backend.domain.learning_activity.infrastructure.subscription.SubscriptionAccessAdapter;
import com.wanted.backend.domain.learning_activity.infrastructure.subscription.SubscriptionReferenceJpaEntity;
import com.wanted.backend.domain.learning_activity.infrastructure.subscription.SpringDataSubscriptionAccessRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // 스키마는 아래 @Sql로 직접 넣는다. Flyway를 켜두면 MySQL 전용인 V1__baseline.sql을
        // H2에서 실행하려다 문법 오류로 컨텍스트 로딩이 실패한다.
        "spring.flyway.enabled=false"
})
@EntityScan(basePackageClasses = {
        VideoProgressJpaEntity.class,
        EnrollmentReferenceJpaEntity.class,
        SubscriptionReferenceJpaEntity.class,
        CourseSectionReferenceEntity.class,
        LessonReferenceEntity.class,
        CatalogCourseReferenceEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        SpringDataVideoProgressRepository.class,
        CourseSectionReferenceRepository.class,
        LessonReferenceRepository.class,
        CatalogCourseReferenceRepository.class,
        SpringDataEnrollmentAccessRepository.class,
        SpringDataSubscriptionAccessRepository.class
})
@Import({
        VideoCatalogAdapter.class,
        EnrollmentAccessAdapter.class,
        SubscriptionAccessAdapter.class,
        CourseProgressQueryAdapter.class,
        VideoProgressRepositoryAdapter.class,
        VideoProgressInserter.class,
        LearningActivityAdapterTest.TestConfig.class
})
// 진도 첫 저장은 별도 트랜잭션(REQUIRES_NEW)에서 INSERT하므로, 준비 데이터가 테스트 트랜잭션에
// 묶여 있으면 두 트랜잭션이 같은 행을 두고 락 경합에 빠진다. ISOLATED로 미리 커밋해둔다.
@Sql(
        scripts = {
                "/sql/learning_activity_adapter_schema.sql",
                "/sql/learning_activity_adapter_data.sql"
        },
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class LearningActivityAdapterTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        VideoPlayUrlPort videoPlayUrlPort() {
            return s3Key -> "https://stream.example.com/" + s3Key;
        }
    }

    @Autowired
    private VideoCatalogAdapter videoCatalogAdapter;

    @Autowired
    private VideoProgressRepositoryAdapter videoProgressRepositoryAdapter;

    @Autowired
    private SpringDataVideoProgressRepository springDataVideoProgressRepository;

    @Autowired
    private CourseProgressQueryAdapter courseProgressQueryAdapter;

    @Autowired
    private EnrollmentAccessAdapter enrollmentAccessAdapter;

    @Autowired
    private SubscriptionAccessAdapter subscriptionAccessAdapter;

    @Test
    void 영상_카탈로그_어댑터가_영상_접근_정보를_조회한다() {
        Optional<VideoAccessInfo> result = videoCatalogAdapter.findByVideoId(10L);

        assertThat(result).isPresent();
        VideoAccessInfo accessInfo = result.get();
        assertThat(accessInfo.videoId()).isEqualTo(10L);
        assertThat(accessInfo.courseId()).isEqualTo(20L);
        assertThat(accessInfo.courseStatus()).isEqualTo("PUBLISHED");
        assertThat(accessInfo.coursePrice()).isEqualTo(10000);
        assertThat(accessInfo.preview()).isTrue();
        assertThat(accessInfo.s3Key()).isEqualTo("videos/10.mp4");
        assertThat(accessInfo.streamingUrl()).isEqualTo("https://stream.example.com/videos/10.mp4");
        assertThat(accessInfo.durationSeconds()).isEqualTo(300);
    }

    @Test
    void 영상_카탈로그_어댑터가_s3Key_없는_레거시_레슨은_video_url로_폴백한다() {
        Optional<VideoAccessInfo> result = videoCatalogAdapter.findByVideoId(11L);

        assertThat(result).isPresent();
        VideoAccessInfo accessInfo = result.get();
        assertThat(accessInfo.s3Key()).isNull();
        assertThat(accessInfo.streamingUrl()).isEqualTo("https://legacy.example.com/video.m3u8");
    }

    @Test
    void 영상_진도_저장소_어댑터가_진도_정보를_조회한다() {
        Optional<VideoProgress> result = videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 10L);

        assertThat(result).isPresent();
        VideoProgress progress = result.get();
        assertThat(progress.id()).isEqualTo(100L);
        assertThat(progress.memberId()).isEqualTo(1L);
        assertThat(progress.courseId()).isEqualTo(20L);
        assertThat(progress.videoId()).isEqualTo(10L);
        assertThat(progress.lastPositionSec()).isEqualTo(42);
        assertThat(progress.watchTimeSec()).isEqualTo(120);
        assertThat(progress.completed()).isTrue();
    }

    @Test
    void 강의_진도_조회_어댑터가_미리보기_레슨을_제외하고_강의_진도_정보를_조회한다() {
        // 레슨 10(섹션 0, 레슨 0)은 미리보기라 진도 집계에서 제외되어야 한다 — VideoCatalogAdapter의
        // "첫 섹션 첫 레슨" 휴리스틱과 동일한 기준. 레슨 11만 남는다.
        CourseProgressQueryPort.CourseProgressData progress =
                courseProgressQueryAdapter.findByMemberIdAndCourseId(1L, 20L);

        assertThat(progress.courseId()).isEqualTo(20L);
        assertThat(progress.lessons()).hasSize(1);
        assertThat(progress.lessons()).noneMatch(l -> l.videoId() == 10L);

        var lesson11 = progress.lessons().stream().filter(l -> l.videoId() == 11L).findFirst().orElseThrow();
        assertThat(lesson11.completed()).isFalse();
        assertThat(lesson11.lastPositionSeconds()).isZero();
    }

    @Test
    void 강의_진도_조회_어댑터가_미리보기_레슨만_있으면_빈_진도_목록을_반환한다() {
        // 강의 21은 레슨이 미리보기(섹션 0, 레슨 0) 하나뿐이다 — 분모 보정 후 lessons가 비어야 한다.
        CourseProgressQueryPort.CourseProgressData progress =
                courseProgressQueryAdapter.findByMemberIdAndCourseId(1L, 21L);

        assertThat(progress.courseId()).isEqualTo(21L);
        assertThat(progress.lessons()).isEmpty();
    }

    @Test
    void 영상_진도_저장소_어댑터가_마지막_재생_위치를_저장한다() {
        // 진도 행이 아직 없는 영상(11) — INSERT 경로
        VideoProgress progress = VideoProgress.empty(1L, 20L, 11L)
                .updateLastPosition(142);

        VideoProgress saved = videoProgressRepositoryAdapter.save(progress);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.memberId()).isEqualTo(1L);
        assertThat(saved.courseId()).isEqualTo(20L);
        assertThat(saved.videoId()).isEqualTo(11L);
        assertThat(saved.lastPositionSec()).isEqualTo(142);
        assertThat(saved.watchTimeSec()).isZero();
        assertThat(saved.completed()).isFalse();
    }

    @Test
    void 영상_진도_저장소_어댑터가_이미_있는_행에_중복_INSERT하지_않고_갱신한다() {
        // 첫 시청 시 동시 요청이 모두 "진도 없음"으로 읽고 각자 INSERT를 시도하던 상황.
        // 경합에 밀린 요청이 빈 진도(id=null)를 들고 오더라도 기존 행(100)을 갱신해야 하고,
        // 중복 행이 생기면 이후 findByMemberIdAndVideoId가 NonUniqueResultException으로 터진다.
        VideoProgress raced = VideoProgress.empty(1L, 20L, 10L)
                .updateLastPosition(77);

        VideoProgress saved = videoProgressRepositoryAdapter.save(raced);

        assertThat(saved.id()).isEqualTo(100L);
        assertThat(saved.lastPositionSec()).isEqualTo(77);
        assertThat(springDataVideoProgressRepository.findByMemberIdAndCourseId(1L, 20L))
                .hasSize(1);
        assertThat(videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 10L))
                .isPresent();
    }

    @Test
    void 영상_진도_저장소_어댑터가_경합이_아닌_진짜_제약_위반은_삼키지_않고_그대로_던진다() {
        // 기존 행이 없는 (member, video)에 course_id=null로 INSERT → NOT NULL 위반.
        // 유니크 경합이 아니므로 복구하지 않고, 원래 제약 위반(C001)이 그대로 상승해
        // 진짜 원인이 로그에 드러나야 한다.
        VideoProgress invalid = new VideoProgress(null, 1L, null, 999L, 0, 0, false, null);

        assertThatThrownBy(() -> videoProgressRepositoryAdapter.save(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 999L)).isEmpty();
    }

    @Test
    void 영상_진도_저장소_어댑터가_기존_행이_있어도_유니크_경합이_아닌_위반은_전파하고_기존_행을_덮어쓰지_않는다() {
        // 기존 행(100: member1/video10, watch_time=120, last_position=42)이 있는 상태에서
        // INSERT 경로(id=null)로 course_id=null 요청이 오면 NOT NULL 위반이다. 이는 유니크
        // 경합이 아니므로 복구 대상이 아니고, 예외를 그대로 전파하며 기존 행을 절대 덮어써선 안 된다.
        VideoProgress invalidForExistingRow = new VideoProgress(null, 1L, null, 10L, 0, 999, true, null);

        assertThatThrownBy(() -> videoProgressRepositoryAdapter.save(invalidForExistingRow))
                .isInstanceOf(DataIntegrityViolationException.class);

        VideoProgress unchanged = videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 10L).orElseThrow();
        assertThat(unchanged.watchTimeSec()).isEqualTo(120);
        assertThat(unchanged.lastPositionSec()).isEqualTo(42);
    }

    @Test
    void 영상_진도_저장소_어댑터가_시청_시간을_저장한다() {
        VideoProgress progress = videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 10L)
                .orElseThrow()
                .addWatchTime(30);

        VideoProgress saved = videoProgressRepositoryAdapter.save(progress);

        assertThat(saved.id()).isEqualTo(100L);
        assertThat(saved.lastPositionSec()).isEqualTo(42);
        assertThat(saved.watchTimeSec()).isEqualTo(150);
        assertThat(saved.completed()).isTrue();
    }

    @Test
    void 영상_진도_저장소_어댑터가_완료_상태를_저장한다() {
        VideoProgress progress = videoProgressRepositoryAdapter.findByMemberIdAndVideoId(1L, 10L)
                .orElseThrow()
                .complete(LocalDateTime.now());

        VideoProgress saved = videoProgressRepositoryAdapter.save(progress);

        assertThat(saved.id()).isEqualTo(100L);
        assertThat(saved.completed()).isTrue();
        assertThat(saved.completedAt()).isNotNull();
    }

    @Test
    void 수강권_접근_어댑터가_활성_수강권을_확인한다() {
        boolean result = enrollmentAccessAdapter.hasActiveEnrollment(1L, 20L);

        assertThat(result).isTrue();
    }

    @Test
    void 구독권_접근_어댑터가_활성_구독권을_확인한다() {
        boolean result = subscriptionAccessAdapter.hasActiveSubscription(1L);

        assertThat(result).isTrue();
    }
}
