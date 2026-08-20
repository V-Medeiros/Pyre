package com.vesta.api.session;

import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionReconciler {

    private final FocusSessionRepository sessions;
    private final SessionService service;
    private final Clock clock;

    public SessionReconciler(FocusSessionRepository sessions, SessionService service, Clock clock) {
        this.sessions = sessions;
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${vesta.sessions.reconcile-delay:PT15S}")
    public void reconcileExpired() {
        sessions.findDueIds(Instant.now(clock), PageRequest.of(0, 100))
                .forEach(service::reconcileOne);
    }
}

