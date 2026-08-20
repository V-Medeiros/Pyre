package com.vesta.api.session;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FocusSessionRepository extends JpaRepository<FocusSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FocusSession s where s.id = :id")
    Optional<FocusSession> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FocusSession s left join fetch s.task where s.id = :id and s.user.id = :userId")
    Optional<FocusSession> findOwnedForUpdate(@Param("id") String id, @Param("userId") UUID userId);

    @Query("select s from FocusSession s left join fetch s.task where s.id = :id and s.user.id = :userId")
    Optional<FocusSession> findOwned(@Param("id") String id, @Param("userId") UUID userId);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "and s.status in (com.vesta.api.session.SessionStatus.RUNNING, com.vesta.api.session.SessionStatus.PAUSED)")
    Optional<FocusSession> findActive(@Param("userId") UUID userId);

    @Query("select s.id from FocusSession s where s.status = com.vesta.api.session.SessionStatus.RUNNING "
            + "and s.currentDeadlineAt <= :now order by s.currentDeadlineAt")
    List<String> findDueIds(@Param("now") Instant now, Pageable pageable);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "order by s.startedAt desc, s.id desc")
    List<FocusSession> findHistory(@Param("userId") UUID userId, Pageable pageable);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "and (s.startedAt < :startedAt or (s.startedAt = :startedAt and s.id < :id)) "
            + "order by s.startedAt desc, s.id desc")
    List<FocusSession> findHistoryAfter(@Param("userId") UUID userId,
                                        @Param("startedAt") Instant startedAt,
                                        @Param("id") String id, Pageable pageable);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "and s.localDate between :from and :to order by s.startedAt desc")
    List<FocusSession> findBetween(@Param("userId") UUID userId,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct s.localDate from FocusSession s where s.user.id = :userId "
            + "and s.status = com.vesta.api.session.SessionStatus.COMPLETED order by s.localDate")
    List<LocalDate> findCompletedDates(@Param("userId") UUID userId);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "and s.updatedAt > :since and s.updatedAt <= :until order by s.updatedAt, s.id")
    List<FocusSession> findChanges(@Param("userId") UUID userId, @Param("since") Instant since,
                                   @Param("until") Instant until);

    @Query("select s from FocusSession s left join fetch s.task where s.user.id = :userId "
            + "order by s.updatedAt, s.id")
    List<FocusSession> findAllOwned(@Param("userId") UUID userId);
}
