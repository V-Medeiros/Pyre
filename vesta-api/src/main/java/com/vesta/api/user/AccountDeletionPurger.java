package com.vesta.api.user;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDeletionPurger {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public AccountDeletionPurger(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(cron = "${vesta.accounts.purge-cron:0 20 3 * * *}", zone = "UTC")
    @Transactional
    public void purgeExpiredAccounts() {
        Instant cutoff = Instant.now(clock).minus(30, ChronoUnit.DAYS);
        List<UUID> userIds = jdbc.queryForList(
                "select id from users where deleted_at is not null and deleted_at <= :cutoff limit 100",
                Map.of("cutoff", cutoff), UUID.class);
        for (UUID userId : userIds) purge(userId);
    }

    private void purge(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        jdbc.update("delete from session_events where user_id = :userId", params);
        jdbc.update("delete from account_tokens where user_id = :userId", params);
        jdbc.update("delete from refresh_sessions where user_id = :userId", params);
        jdbc.update("delete from import_batches where user_id = :userId", params);
        jdbc.update("delete from focus_sessions where user_id = :userId", params);
        jdbc.update("delete from tasks where user_id = :userId", params);
        jdbc.update("delete from user_preferences where user_id = :userId", params);
        jdbc.update("delete from users where id = :userId", params);
    }
}

