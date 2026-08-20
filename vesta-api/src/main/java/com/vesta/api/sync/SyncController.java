package com.vesta.api.sync;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.sync.SyncDtos.SyncResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    @GetMapping("/bootstrap")
    SyncResponse bootstrap(Authentication authentication) {
        return service.bootstrap(CurrentUser.id(authentication));
    }

    @GetMapping("/changes")
    SyncResponse changes(Authentication authentication, @RequestParam String cursor) {
        return service.changes(CurrentUser.id(authentication), cursor);
    }
}

