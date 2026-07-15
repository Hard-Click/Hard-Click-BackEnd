package com.wanted.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Gate 1 · 결정론 규칙 (ArchUnit) — 규칙 카탈로그 §03의 ARCH_* 계열을 코드로 집행한다.
 *
 * 이 테스트가 초록불 = "팀의 클린아키텍처 규칙을 코드가 지키고 있다"는 기계 증명(재현·감사 가능).
 * 빨간불 = 실제 위반이 생겼다는 신호 → 그 자체가 리뷰 루프의 산출물(고치거나, 의도적이면 예외 등록).
 *
 * LLM은 여기에 관여하지 않는다. 정적분석이 원천적으로 못 잡는 의미규칙(중복 재발명·N+1)만 Gate 2(Judge)가 맡는다.
 */
@DisplayName("Gate 1 · 아키텍처 규칙 (ArchUnit)")
class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.wanted.backend";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        // 테스트 코드는 제외하고 운영 코드(main)만 분석 대상으로 삼는다.
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("ARCH_001: presentation(컨트롤러)은 domain.repository를 직접 호출하지 않는다 → Service 경유")
    void presentationMustNotAccessRepositoryDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..presentation..")
                .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
                .because("컨트롤러는 비즈니스 로직을 담지 않는다 — 조회/명령은 application(Service)를 경유해야 한다");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("ARCH_002: domain.model은 Spring·JPA에 의존하지 않는다 (순수 POJO 유지)")
    void domainModelMustNotDependOnFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .because("도메인 모델은 프레임워크·영속성 기술에서 독립적이어야 한다 (JpaEntity는 infrastructure.persistence에 둔다)");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("ARCH_003: application은 infrastructure를 직접 참조하지 않는다 (포트-어댑터 경계)")
    void applicationMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("application은 port(인터페이스)만 알고, 구현(adapter)은 infrastructure가 DI로 주입한다");

        // 기존 위반(PostQueryService→PostCountCache, GetStudyStreakService→GrassStreakProperties)은
        // baseline으로 얼려 known-debt로 관리하고, 신규 위반만 실패시킨다 (Q1: baseline-diff-only).
        // 부채는 별도 티켓으로 갚는다. baseline은 archunit_store/에 기록된다.
        FreezingArchRule.freeze(rule).check(productionClasses);
    }
}
