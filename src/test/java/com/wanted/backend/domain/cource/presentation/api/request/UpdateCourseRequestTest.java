package com.wanted.backend.domain.cource.presentation.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanted.backend.domain.cource.application.command.UpdateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 강의 수정 요청 역직렬화 회귀 테스트.
 * <p>
 * 과거 버그: 요청 DTO의 식별자 필드명이 {@code id}였는데 FE는 조회(GET) 응답과 동일하게
 * {@code sectionId}/{@code lessonId}로 전송 → Jackson이 바인딩에 실패해 null이 되고,
 * 백엔드가 모든 섹션/레슨을 신규로 판정해 기존 커리큘럼을 삭제·재생성 → 영상(s3_key) 소실.
 * FE payload의 필드명이 실제로 식별자에 바인딩되는지 고정한다.
 */
class UpdateCourseRequestTest {

    // 앱과 동일하게 record 파라미터명 기반 바인딩(ParameterNamesModule)을 사용한다.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void FE_payload의_sectionId_lessonId가_식별자로_바인딩된다() throws Exception {
        // given — FE가 실제로 보내는 형태(기존 항목은 sectionId/lessonId, 신규 항목은 키 생략)
        String json = """
                {
                  "title": "수학I·II 개념 완성",
                  "subject": "MATH_1",
                  "priceType": "PAID",
                  "price": 50000,
                  "level": "중급",
                  "sections": [
                    {
                      "sectionId": 12,
                      "title": "1주차",
                      "orderIndex": 0,
                      "lessons": [
                        { "lessonId": 625, "title": "지수와 로그", "description": "지수와 로그", "orderIndex": 0, "durationSeconds": 596 },
                        { "title": "새 강의", "description": "새 강의", "orderIndex": 1, "durationSeconds": 0 }
                      ]
                    },
                    {
                      "title": "신규 섹션",
                      "orderIndex": 1,
                      "lessons": []
                    }
                  ]
                }
                """;

        // when
        UpdateCourseRequest request = objectMapper.readValue(json, UpdateCourseRequest.class);
        UpdateCourseCommand command = request.toCommand(104L, 1L);

        // then — 기존 섹션/레슨은 식별자가 살아있어야 재생성이 아니라 제자리 갱신 경로를 탄다
        assertThat(request.sections()).hasSize(2);
        UpdateSectionRequest section = request.sections().get(0);
        assertThat(section.sectionId()).isEqualTo(12L);
        assertThat(section.lessons().get(0).lessonId()).isEqualTo(625L);
        // 신규 레슨은 식별자 키가 없으므로 null → 신규로 처리
        assertThat(section.lessons().get(1).lessonId()).isNull();
        // 신규 섹션은 sectionId 키가 없으므로 null → 신규로 처리
        assertThat(request.sections().get(1).sectionId()).isNull();

        // command 매핑까지 식별자가 보존되는지 확인
        UpdateCourseCommand.SectionCommand sectionCommand = command.sections().get(0);
        assertThat(sectionCommand.id()).isEqualTo(12L);
        assertThat(sectionCommand.lessons().get(0).id()).isEqualTo(625L);
        assertThat(sectionCommand.lessons().get(1).id()).isNull();
        assertThat(command.sections().get(1).id()).isNull();
    }
}
