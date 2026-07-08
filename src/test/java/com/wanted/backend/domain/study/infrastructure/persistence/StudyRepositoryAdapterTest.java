package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.Study;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import(StudyRepositoryAdapter.class)
class StudyRepositoryAdapterTest {

    @Autowired
    private StudyRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("이미 저장된 스터디를 다시 save()하면 새 행이 아니라 기존 행이 갱신된다")
    void save_onExistingStudy_updatesInPlace_notInsertsNewRow() {
        // given
        Study created = Study.create(1L, "제목", "MATH_1", 5, "내용");
        Study saved = adapter.save(created);
        em.flush();
        em.clear();

        Study loaded = adapter.findById(saved.getId()).orElseThrow();
        Study updated = Study.restore(loaded.getId(), loaded.getHostId(), "수정된 제목", loaded.getSubject(),
                loaded.getContent(), loaded.getMaxCount(), loaded.getCurrentCount(), loaded.getStatus(),
                loaded.getCreatedAt(), LocalDateTime.now());

        // when
        adapter.save(updated);
        em.flush();
        em.clear();

        // then
        long rowCount = em.getEntityManager()
                .createQuery("SELECT COUNT(s) FROM StudyJpaEntity s", Long.class)
                .getSingleResult();
        assertThat(rowCount).isEqualTo(1);

        Study reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("수정된 제목");
    }
}
