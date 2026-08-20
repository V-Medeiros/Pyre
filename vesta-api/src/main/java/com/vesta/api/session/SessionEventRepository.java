package com.vesta.api.session;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionEventRepository extends JpaRepository<SessionEvent, UUID> {
}

