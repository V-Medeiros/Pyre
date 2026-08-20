package com.vesta.api.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.vesta.api.user.UserAccount;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FocusSessionTest {

    private final Instant start = Instant.parse("2026-08-20T02:58:00Z");
    private final UserAccount user = new UserAccount(UUID.randomUUID(), "user@example.com", "hash",
            "Vesta User", "America/Sao_Paulo", "pt-BR", start);

    @Test
    void completesAtServerDeadlineAndUsesCapturedTimezone() {
        FocusSession session = FocusSession.start("session_1", user, null, 300, start);

        assertThat(session.reconcile(start.plusSeconds(301))).isTrue();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getEndedAt()).isEqualTo(start.plusSeconds(300));
        assertThat(session.getActualFocusSeconds()).isEqualTo(300);
        assertThat(session.getLocalDate()).isEqualTo("2026-08-20");
    }

    @Test
    void pauseAndResumePreserveOnlyFocusedTime() {
        FocusSession session = FocusSession.start("session_2", user, null, 300, start);

        assertThat(session.pause(start.plusSeconds(60))).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(session.secondsRemainingAt(start.plusSeconds(600))).isEqualTo(240);

        assertThat(session.resume(start.plusSeconds(600))).isTrue();
        assertThat(session.getCurrentDeadlineAt()).isEqualTo(start.plusSeconds(840));
        assertThat(session.reconcile(start.plusSeconds(839))).isFalse();
        assertThat(session.reconcile(start.plusSeconds(840))).isTrue();
    }

    @Test
    void abandoningStoresActualElapsedFocus() {
        FocusSession session = FocusSession.start("session_3", user, null, 300, start);

        assertThat(session.abandon(start.plusSeconds(42))).isTrue();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ABANDONED);
        assertThat(session.getActualFocusSeconds()).isEqualTo(42);
        assertThat(session.reconcile(start.plusSeconds(600))).isFalse();
    }

    @Test
    void duplicateTransitionsAreNoOps() {
        FocusSession session = FocusSession.start("session_4", user, null, 300, start);

        assertThat(session.pause(start.plusSeconds(10))).isTrue();
        assertThat(session.pause(start.plusSeconds(20))).isFalse();
        assertThat(session.getAccumulatedFocusSeconds()).isEqualTo(10);
        assertThat(session.resume(start.plusSeconds(30))).isTrue();
        assertThat(session.resume(start.plusSeconds(40))).isFalse();
    }
}

