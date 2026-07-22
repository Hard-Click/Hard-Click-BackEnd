package com.wanted.backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wanted.backend.domain.grass.application.mapper.MonthlyGrassViewMapper;
import com.wanted.backend.domain.grass.application.mapper.YearlyGrassViewMapper;
import com.wanted.backend.domain.grass.application.query.GetGrassViewQuery;
import com.wanted.backend.domain.grass.application.query.GetLessonGrassQuery;
import com.wanted.backend.domain.grass.application.query.GetMonthlyGrassQuery;
import com.wanted.backend.domain.grass.application.query.GetYearlyGrassQuery;
import com.wanted.backend.domain.grass.application.service.GetGrassViewService;
import com.wanted.backend.domain.grass.application.service.GetLessonGrassService;
import com.wanted.backend.domain.grass.application.service.GetMonthlyGrassService;
import com.wanted.backend.domain.grass.application.service.GetYearlyGrassService;
import com.wanted.backend.domain.grass.application.usecase.GetGrassViewUseCase;
import com.wanted.backend.domain.grass.application.usecase.GetLessonGrassUseCase;
import com.wanted.backend.domain.grass.application.usecase.GetMonthlyGrassUseCase;
import com.wanted.backend.domain.grass.application.usecase.GetYearlyGrassUseCase;
import com.wanted.backend.domain.grass.domain.model.LessonGrassStat;
import com.wanted.backend.domain.grass.domain.model.YearlyGrassStat;
import com.wanted.backend.domain.grass.domain.policy.GrassViewModePolicy;
import com.wanted.backend.domain.grass.domain.policy.LessonGrassLevelPolicy;
import com.wanted.backend.domain.grass.domain.policy.MonthlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.policy.YearlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.repository.LessonGrassRepository;
import com.wanted.backend.domain.grass.domain.repository.MonthlyGrassRepository;
import com.wanted.backend.domain.grass.domain.repository.YearlyGrassRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RedisConfig.cacheManager()와 동일한 ObjectMapper(activateDefaultTyping EVERYTHING) 설정으로
 * 실제 캐싱 대상 응답 객체가 직렬화 -> 역직렬화 라운드트립에서 깨지지 않는지 검증한다.
 *
 * record(=final) 타입을 @Cacheable의 루트 반환값으로 쓰면, DefaultTyping.NON_FINAL에서는
 * 타입 메타데이터가 안 붙는데 GenericJackson2JsonRedisSerializer.deserialize(byte[])는
 * 항상 Object.class를 기대해서 캐시 HIT마다 역직렬화가 실패했다
 * (grass monthly/yearly/lessons, admin dashboard 500의 원인). EVERYTHING으로 바꿔서 해결.
 */
class CacheSerializationRoundTripTest {

    private final GenericJackson2JsonRedisSerializer serializer = buildCacheSerializer();

    private static GenericJackson2JsonRedisSerializer buildCacheSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.wanted.backend")
                        .allowIfSubType("java.util")
                        .allowIfSubType("java.time")
                        .allowIfSubType("java.lang")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Test
    void roundTripsLessonGrassViewList() {
        LessonGrassRepository repository = mock(LessonGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        GetLessonGrassService service = new GetLessonGrassService(repository, new LessonGrassLevelPolicy(4), new YearlyGrassPeriodPolicy(), new MonthlyGrassPeriodPolicy(), clock);
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-03")))
                .thenReturn(List.of(new LessonGrassStat(1L, LocalDate.parse("2026-01-02"), 3)));

        List<GetLessonGrassUseCase.LessonGrassView> original = service.handle(new GetLessonGrassQuery(1L, null));

        assertRoundTrips(original);
    }

    @Test
    void roundTripsMonthlyGrassView() {
        MonthlyGrassRepository repository = mock(MonthlyGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        MonthlyGrassViewMapper mapper = new MonthlyGrassViewMapper(new LessonGrassLevelPolicy(4));
        GetMonthlyGrassService service = new GetMonthlyGrassService(
                repository, mapper, new MonthlyGrassPeriodPolicy(), clock
        );
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-15")))
                .thenReturn(List.of());

        GetMonthlyGrassUseCase.MonthlyGrassView original = service.handle(new GetMonthlyGrassQuery(1L, 2026, 6));

        assertRoundTrips(original);
    }

    @Test
    void roundTripsYearlyGrassView() {
        YearlyGrassRepository repository = mock(YearlyGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        YearlyGrassViewMapper mapper = new YearlyGrassViewMapper(new LessonGrassLevelPolicy(4));
        GetYearlyGrassService service = new GetYearlyGrassService(
                repository, mapper, new YearlyGrassPeriodPolicy(), clock
        );
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-10")))
                .thenReturn(List.of(new YearlyGrassStat(1L, LocalDate.parse("2026-02-01"), 2)));

        GetYearlyGrassUseCase.YearlyGrassView original = service.handle(new GetYearlyGrassQuery(1L, 2026));

        assertRoundTrips(original);
    }

    @Test
    void roundTripsCombinedGrassView() {
        GetMonthlyGrassUseCase monthlyUseCase = mock(GetMonthlyGrassUseCase.class);
        GetYearlyGrassUseCase yearlyUseCase = mock(GetYearlyGrassUseCase.class);
        GetGrassViewService service = new GetGrassViewService(monthlyUseCase, yearlyUseCase, new GrassViewModePolicy());
        when(monthlyUseCase.handle(any(GetMonthlyGrassQuery.class))).thenReturn(new GetMonthlyGrassUseCase.MonthlyGrassView(
                2026, 6, List.of(new GetMonthlyGrassUseCase.MonthlyGrassDayView(
                        LocalDate.parse("2026-06-01"), 2, 2, false
                ))
        ));

        GetGrassViewUseCase.GrassView original = service.handle(new GetGrassViewQuery(1L, "monthly", 2026, 6));

        assertRoundTrips(original);
    }

    // AdminDashboardResult는 AdminDashboardQueryAdapter(JPA 쿼리)를 실제로 거쳐야
    // findRecentReports()/findRecentNotices()의 ArrayList 수집 로직까지 검증되므로,
    // AdminDashboardQueryAdapterCacheSerializationTest(@DataJpaTest)에서 다룬다.

    /**
     * RedisConfig.buildGrassCacheConfigs()가 실제로 만드는 캐시 이름별 직렬화 설정을 그대로 호출해서
     * 검증한다. 테스트에서 직렬화 설정을 따로 재구성하면 RedisConfig의 캐시 이름·타입 매핑이
     * 어긋나도(예: 캐시 이름 오타, 잘못된 타입) 테스트가 못 잡으므로, 프로덕션 코드와 같은
     * 메서드를 거치도록 한다.
     */
    private static final Map<String, RedisCacheConfiguration> GRASS_CACHE_CONFIGS = RedisConfig.buildGrassCacheConfigs(
            RedisCacheConfiguration.defaultCacheConfig(),
            buildPlainObjectMapper()
    );

    @Test
    void typedSerializerRoundTripsMonthlyGrassView() {
        MonthlyGrassRepository repository = mock(MonthlyGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        MonthlyGrassViewMapper mapper = new MonthlyGrassViewMapper(new LessonGrassLevelPolicy(4));
        GetMonthlyGrassService service = new GetMonthlyGrassService(
                repository, mapper, new MonthlyGrassPeriodPolicy(), clock
        );
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-15")))
                .thenReturn(List.of());

        GetMonthlyGrassUseCase.MonthlyGrassView original = service.handle(new GetMonthlyGrassQuery(1L, 2026, 6));

        assertRoundTripsViaCacheConfig("grassMonthly:v3", original);
    }

    @Test
    void typedSerializerRoundTripsYearlyGrassView() {
        YearlyGrassRepository repository = mock(YearlyGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        YearlyGrassViewMapper mapper = new YearlyGrassViewMapper(new LessonGrassLevelPolicy(4));
        GetYearlyGrassService service = new GetYearlyGrassService(
                repository, mapper, new YearlyGrassPeriodPolicy(), clock
        );
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-10")))
                .thenReturn(List.of(new YearlyGrassStat(1L, LocalDate.parse("2026-02-01"), 2)));

        GetYearlyGrassUseCase.YearlyGrassView original = service.handle(new GetYearlyGrassQuery(1L, 2026));

        assertRoundTripsViaCacheConfig("grassYearly:v3", original);
    }

    @Test
    void typedSerializerRoundTripsGrassView() {
        GetMonthlyGrassUseCase monthlyUseCase = mock(GetMonthlyGrassUseCase.class);
        GetYearlyGrassUseCase yearlyUseCase = mock(GetYearlyGrassUseCase.class);
        GetGrassViewService service = new GetGrassViewService(monthlyUseCase, yearlyUseCase, new GrassViewModePolicy());
        when(monthlyUseCase.handle(any(GetMonthlyGrassQuery.class))).thenReturn(new GetMonthlyGrassUseCase.MonthlyGrassView(
                2026, 6, List.of(new GetMonthlyGrassUseCase.MonthlyGrassDayView(
                        LocalDate.parse("2026-06-01"), 2, 2, false
                ))
        ));

        GetGrassViewUseCase.GrassView original = service.handle(new GetGrassViewQuery(1L, "monthly", 2026, 6));

        assertRoundTripsViaCacheConfig("grassView:v3", original);
    }

    @Test
    void typedSerializerRoundTripsLessonGrassViewList() {
        LessonGrassRepository repository = mock(LessonGrassRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        GetLessonGrassService service = new GetLessonGrassService(repository, new LessonGrassLevelPolicy(4), new YearlyGrassPeriodPolicy(), new MonthlyGrassPeriodPolicy(), clock);
        when(repository.findByMemberIdAndDateBetween(1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-03")))
                .thenReturn(List.of(new LessonGrassStat(1L, LocalDate.parse("2026-01-02"), 3)));

        List<GetLessonGrassUseCase.LessonGrassView> original = service.handle(new GetLessonGrassQuery(1L, null));

        assertRoundTripsViaCacheConfig("grassLessons:v3", original);
    }

    /**
     * RedisConfig.buildGrassCacheConfigs()가 cacheName에 대해 실제로 등록한
     * SerializationPair로 직렬화 -> 역직렬화해서, 그 결과가 원본과 같은지 검증한다.
     */
    private void assertRoundTripsViaCacheConfig(String cacheName, Object original) {
        RedisCacheConfiguration cacheConfig = GRASS_CACHE_CONFIGS.get(cacheName);
        assertThat(cacheConfig)
                .as("RedisConfig.buildGrassCacheConfigs()에 '%s' 캐시 설정이 없습니다", cacheName)
                .isNotNull();

        RedisSerializationContext.SerializationPair<Object> pair = cacheConfig.getValueSerializationPair();
        ByteBuffer serialized = pair.write(original);
        Object deserialized = pair.read(serialized);

        assertThat(deserialized).isEqualTo(original);
    }

    private static ObjectMapper buildPlainObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    private void assertRoundTrips(Object original) {
        byte[] serialized = serializer.serialize(original);
        Object deserialized = serializer.deserialize(serialized);

        assertThat(deserialized).isEqualTo(original);
    }
}
