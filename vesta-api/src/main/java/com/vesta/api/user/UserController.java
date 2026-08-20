package com.vesta.api.user;

import com.vesta.api.auth.AuthDtos.UserResponse;
import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.user.UserDtos.AccountExport;
import com.vesta.api.user.UserDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    UserResponse me(Authentication authentication) {
        return service.me(CurrentUser.id(authentication));
    }

    @PatchMapping
    UserResponse update(Authentication authentication,
                        @Valid @RequestBody UpdateProfileRequest request) {
        return service.update(CurrentUser.id(authentication), request);
    }

    @GetMapping("/export")
    AccountExport export(Authentication authentication) {
        return service.export(CurrentUser.id(authentication));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication authentication) {
        service.delete(CurrentUser.id(authentication));
    }
}
