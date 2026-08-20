package com.vesta.api.session;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.session.SessionDtos.SessionPage;
import com.vesta.api.session.SessionDtos.SessionResponse;
import com.vesta.api.session.SessionDtos.StartSessionRequest;
import com.vesta.api.session.SessionDtos.TransitionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/focus-sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SessionResponse start(Authentication auth, @Valid @RequestBody StartSessionRequest request) {
        return service.start(CurrentUser.id(auth), request);
    }

    @GetMapping("/active")
    SessionResponse active(Authentication auth) {
        return service.active(CurrentUser.id(auth));
    }

    @GetMapping("/{id}")
    SessionResponse get(Authentication auth, @PathVariable String id) {
        return service.get(CurrentUser.id(auth), id);
    }

    @GetMapping
    SessionPage list(Authentication auth,
                     @RequestParam(required = false) String cursor,
                     @RequestParam(defaultValue = "50") int limit) {
        return service.list(CurrentUser.id(auth), cursor, limit);
    }

    @PostMapping("/{id}/pause")
    SessionResponse pause(Authentication auth, @PathVariable String id,
                          @Valid @RequestBody(required = false) TransitionRequest request) {
        return service.pause(CurrentUser.id(auth), id, empty(request));
    }

    @PostMapping("/{id}/resume")
    SessionResponse resume(Authentication auth, @PathVariable String id,
                           @Valid @RequestBody(required = false) TransitionRequest request) {
        return service.resume(CurrentUser.id(auth), id, empty(request));
    }

    @PostMapping("/{id}/abandon")
    SessionResponse abandon(Authentication auth, @PathVariable String id,
                            @Valid @RequestBody(required = false) TransitionRequest request) {
        return service.abandon(CurrentUser.id(auth), id, empty(request));
    }

    private TransitionRequest empty(TransitionRequest request) {
        return request == null ? new TransitionRequest(null, null) : request;
    }
}
