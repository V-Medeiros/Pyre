package com.vesta.api.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vesta.api.importer.ImportDtos.LocalStorageImportRequest;
import com.vesta.api.preferences.UserPreferencesRepository;
import com.vesta.api.session.FocusSessionRepository;
import com.vesta.api.session.SessionEventRepository;
import com.vesta.api.task.TaskRepository;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalStorageImportServiceTest {

    @Test
    void replayReturnsOriginalReportWithoutImportingAgain() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T12:00:00Z");
        UserAccount user = new UserAccount(userId, "user@example.com", "hash", null,
                "UTC", "en-US", now);
        ImportBatchRepository batches = mock(ImportBatchRepository.class);
        UserRepository users = mock(UserRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        FocusSessionRepository sessions = mock(FocusSessionRepository.class);
        SessionEventRepository events = mock(SessionEventRepository.class);
        UserPreferencesRepository preferences = mock(UserPreferencesRepository.class);
        ImportBatch original = new ImportBatch(user, "browser.12345678", 3, 5, 1, 0, now);
        when(batches.findByUserIdAndIdempotencyKey(userId, "browser.12345678"))
                .thenReturn(Optional.of(original));
        LocalStorageImportService service = new LocalStorageImportService(batches, users, tasks,
                sessions, events, preferences, Clock.fixed(now, ZoneOffset.UTC));

        var result = service.importData(userId, "browser.12345678",
                new LocalStorageImportRequest(List.of(), List.of(), null, null, null, null, null));

        assertThat(result.replayed()).isTrue();
        assertThat(result.importedTasks()).isEqualTo(3);
        assertThat(result.importedSessions()).isEqualTo(5);
        verifyNoInteractions(users, tasks, sessions, events, preferences);
    }
}

