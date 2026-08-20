package com.vesta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.preferences.UserPreferences;
import com.vesta.api.preferences.UserPreferencesRepository;
import com.vesta.api.session.SessionDtos.StartSessionRequest;
import com.vesta.api.session.SessionService;
import com.vesta.api.task.TaskDtos.CreateTaskRequest;
import com.vesta.api.task.TaskService;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PostgresContextTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired UserRepository users;
    @Autowired UserPreferencesRepository preferences;
    @Autowired TaskService taskService;
    @Autowired SessionService sessionService;

    private UUID firstUserId;
    private UUID secondUserId;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void createUsers() {
        Instant now = Instant.parse("2026-08-20T12:00:00Z");
        UserAccount first = users.save(new UserAccount(UUID.randomUUID(), UUID.randomUUID() + "@example.com",
                "hash", "First", "UTC", "en-US", now));
        UserAccount second = users.save(new UserAccount(UUID.randomUUID(), UUID.randomUUID() + "@example.com",
                "hash", "Second", "UTC", "en-US", now));
        preferences.save(new UserPreferences(first, now));
        preferences.save(new UserPreferences(second, now));
        firstUserId = first.getId();
        secondUserId = second.getId();
    }

    @Test
    void contextLoadsWithFlywaySchema() {
    }

    @Test
    void taskOwnershipIsEnforced() {
        var task = taskService.create(firstUserId, new CreateTaskRequest("task_private", "Private work"));

        assertThatThrownBy(() -> taskService.get(secondUserId, task.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void concurrentStartsProduceOnlyOneActiveSession() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            CompletableFuture<?> first = CompletableFuture.runAsync(() -> attemptStart(
                    new StartSessionRequest("session_concurrent_1", 5, null, "device-1"),
                    ready, start, successes), executor);
            CompletableFuture<?> second = CompletableFuture.runAsync(() -> attemptStart(
                    new StartSessionRequest("session_concurrent_2", 5, null, "device-2"),
                    ready, start, successes), executor);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);
        }
        assertThat(successes).hasValue(1);
    }

    private void attemptStart(StartSessionRequest request, CountDownLatch ready,
                              CountDownLatch start, AtomicInteger successes) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            sessionService.start(firstUserId, request);
            successes.incrementAndGet();
        } catch (RuntimeException exception) {
            // One competing transaction is expected to conflict.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
