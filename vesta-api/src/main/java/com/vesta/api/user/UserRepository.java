package com.vesta.api.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    @Query("select u from UserAccount u where lower(u.email) = lower(:email) and u.deletedAt is null")
    Optional<UserAccount> findActiveByEmail(@Param("email") String email);

    @Query("select u from UserAccount u where u.id = :id and u.deletedAt is null")
    Optional<UserAccount> findActiveById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id and u.deletedAt is null")
    Optional<UserAccount> findActiveByIdForUpdate(@Param("id") UUID id);
}

