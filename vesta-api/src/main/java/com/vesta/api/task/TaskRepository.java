package com.vesta.api.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, String> {

    @Query("select t from Task t where t.id = :id and t.user.id = :userId")
    Optional<Task> findOwnedIncludingDeleted(@Param("id") String id, @Param("userId") UUID userId);

    @Query("select t from Task t where t.id = :id and t.user.id = :userId and t.deletedAt is null")
    Optional<Task> findActiveOwned(@Param("id") String id, @Param("userId") UUID userId);

    List<Task> findAllByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(UUID userId);

    @Query("select t from Task t where t.user.id = :userId and t.updatedAt > :since "
            + "and t.updatedAt <= :until order by t.updatedAt, t.id")
    List<Task> findChanges(@Param("userId") UUID userId, @Param("since") java.time.Instant since,
                           @Param("until") java.time.Instant until);

    @Query("select t from Task t where t.user.id = :userId order by t.updatedAt, t.id")
    List<Task> findAllOwned(@Param("userId") UUID userId);

    @Query(value = "select exists(select 1 from focus_sessions s where s.user_id = :userId "
            + "and s.task_id = :taskId and s.status in ('RUNNING','PAUSED'))", nativeQuery = true)
    boolean hasActiveSession(@Param("userId") UUID userId, @Param("taskId") String taskId);
}
