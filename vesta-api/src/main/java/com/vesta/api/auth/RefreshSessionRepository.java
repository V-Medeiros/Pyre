package com.vesta.api.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshSession r join fetch r.user where r.tokenHash = :hash")
    Optional<RefreshSession> findByHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshSession r set r.revokedAt = :now, r.lastUsedAt = :now "
            + "where r.user.id = :userId and r.revokedAt is null")
    int revokeAll(@Param("userId") UUID userId, @Param("now") Instant now);
}

