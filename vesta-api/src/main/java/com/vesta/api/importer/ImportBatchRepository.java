package com.vesta.api.importer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    Optional<ImportBatch> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<ImportBatch> findByIdAndUserId(UUID id, UUID userId);
}
