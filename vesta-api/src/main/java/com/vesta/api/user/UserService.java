package com.vesta.api.user;

import com.vesta.api.auth.AuthDtos.UserResponse;
import com.vesta.api.auth.RefreshSessionRepository;
import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.ConflictException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.sync.SyncService;
import com.vesta.api.user.UserDtos.AccountExport;
import com.vesta.api.user.UserDtos.UpdateProfileRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final RefreshSessionRepository refreshSessions;
    private final SyncService sync;
    private final Clock clock;

    public UserService(UserRepository users, RefreshSessionRepository refreshSessions,
                       SyncService sync, Clock clock) {
        this.users = users;
        this.refreshSessions = refreshSessions;
        this.sync = sync;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest request) {
        UserAccount user = find(userId);
        if (request.version() != null && request.version() != user.getVersion()) {
            throw new ConflictException("stale_version", "Profile changed on another device.");
        }
        String timezone = request.timezone() == null ? user.getTimezone() : request.timezone();
        validateTimezone(timezone);
        user.updateProfile(request.displayName() == null ? user.getDisplayName() : request.displayName().trim(),
                timezone, request.locale() == null ? user.getLocale() : request.locale(), Instant.now(clock));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AccountExport export(UUID userId) {
        UserAccount user = find(userId);
        return new AccountExport(Instant.now(clock), UserResponse.from(user), sync.bootstrap(userId));
    }

    @Transactional
    public void delete(UUID userId) {
        UserAccount user = users.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
        Instant now = Instant.now(clock);
        user.scheduleDeletion(now);
        refreshSessions.revokeAll(userId, now);
    }

    private UserAccount find(UUID userId) {
        return users.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException exception) {
            throw new BadRequestException("invalid_timezone", "Use a valid IANA timezone.");
        }
    }
}

