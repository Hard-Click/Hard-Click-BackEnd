package com.wanted.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileProcessing-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // 퀴즈 제출 내용을 AI(Python) 서버로 비동기 전송하는 전용 풀.
    // AI 서버 지연이 알림 등 다른 비동기 작업에 전파되지 않도록 분리한다.
    @Bean(name = "quizAiExecutor")
    public Executor quizAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("QuizAi-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // 수강 시작 시 AI 스케줄러(Python) 즉시 생성 호출 전용 풀.
    // CP-SAT 최적화는 수 초~수십 초 걸릴 수 있어(readTimeout 60초) 다른 비동기 작업과 분리한다.
    @Bean(name = "schedulerAiExecutor")
    public Executor schedulerAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("SchedulerAi-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 포화 시 CallerRunsPolicy는 수강 요청 스레드에서 최대 60초 Python 호출을 동기 실행해
        // 수강 API를 지연/차단시킨다. 즉시 생성은 best-effort이고 미생성분은 주간 배치(weekly_reflow)가
        // 백업하므로, 포화 시엔 요청 스레드를 막지 않고 폐기 + 경고 로그만 남긴다.
        AtomicLong schedulerAiDiscarded = new AtomicLong();
        executor.setRejectedExecutionHandler((r, e) ->
                log.warn("schedulerAiExecutor 포화 — 즉시 스케줄 생성 작업 폐기(주간 배치가 백업). 누적 폐기 건수={}",
                        schedulerAiDiscarded.incrementAndGet()));
        executor.initialize();
        return executor;
    }

    // STOMP 구독 시점의 자동 읽음 처리(DB write)를 clientInboundChannel 스레드 풀과 분리한다.
    // preSend는 한정된 STOMP 스레드 풀에서 동기 실행되므로, 여기서 DB I/O를 직접 하면
    // 트래픽이 몰릴 때 스레드 풀 고갈로 전체 채팅 기능이 멎을 수 있다.
    @Bean(name = "chatReadExecutor")
    public Executor chatReadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ChatRead-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
