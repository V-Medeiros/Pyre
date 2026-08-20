package com.vesta.api.session;

import com.vesta.api.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "session_events")
public class SessionEvent {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private FocusSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(nullable = false, length = 16)
    private String type;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "device_id", length = 100)
    private String deviceId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected SessionEvent() {
    }

    public SessionEvent(FocusSession session, String type, Instant occurredAt, String deviceId) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.user = session.getUser();
        this.type = type;
        this.occurredAt = occurredAt;
        this.deviceId = deviceId;
        this.metadata = Map.of();
    }
}

