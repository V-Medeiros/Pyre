package com.vesta.api.preferences;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.preferences.PreferencesDtos.PreferencesResponse;
import com.vesta.api.preferences.PreferencesDtos.UpdatePreferencesRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
public class PreferencesController {

    private final PreferencesService service;

    public PreferencesController(PreferencesService service) {
        this.service = service;
    }

    @GetMapping
    PreferencesResponse get(Authentication authentication) {
        return service.get(CurrentUser.id(authentication));
    }

    @PatchMapping
    PreferencesResponse update(Authentication authentication,
                               @Valid @RequestBody UpdatePreferencesRequest request) {
        return service.update(CurrentUser.id(authentication), request);
    }
}

