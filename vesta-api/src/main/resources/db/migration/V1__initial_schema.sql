CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80),
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-US',
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_users_email_active ON users (LOWER(email)) WHERE deleted_at IS NULL;

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(160),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_refresh_sessions_user ON refresh_sessions(user_id);

CREATE TABLE account_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    default_duration_minutes INTEGER NOT NULL DEFAULT 25 CHECK (default_duration_minutes BETWEEN 5 AND 120),
    sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE tasks (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_tasks_user_updated ON tasks(user_id, updated_at DESC, id DESC);

CREATE TABLE focus_sessions (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    task_id VARCHAR(100) REFERENCES tasks(id),
    task_title_snapshot VARCHAR(120),
    status VARCHAR(16) NOT NULL CHECK (status IN ('RUNNING', 'PAUSED', 'COMPLETED', 'ABANDONED')),
    planned_duration_seconds INTEGER NOT NULL CHECK (planned_duration_seconds BETWEEN 300 AND 7200),
    accumulated_focus_seconds INTEGER NOT NULL DEFAULT 0 CHECK (accumulated_focus_seconds >= 0),
    actual_focus_seconds INTEGER CHECK (actual_focus_seconds >= 0),
    started_at TIMESTAMPTZ NOT NULL,
    state_changed_at TIMESTAMPTZ NOT NULL,
    current_deadline_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    session_timezone VARCHAR(64) NOT NULL,
    local_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_focus_sessions_one_active ON focus_sessions(user_id)
    WHERE status IN ('RUNNING', 'PAUSED');
CREATE INDEX idx_focus_sessions_due ON focus_sessions(status, current_deadline_at)
    WHERE status = 'RUNNING';
CREATE INDEX idx_focus_sessions_history ON focus_sessions(user_id, ended_at DESC, id DESC);
CREATE INDEX idx_focus_sessions_daily ON focus_sessions(user_id, local_date) WHERE status = 'COMPLETED';

CREATE TABLE session_events (
    id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL REFERENCES focus_sessions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    device_id VARCHAR(100),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_session_events_session ON session_events(session_id, occurred_at);

CREATE TABLE import_batches (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(100) NOT NULL,
    imported_tasks INTEGER NOT NULL DEFAULT 0,
    imported_sessions INTEGER NOT NULL DEFAULT 0,
    ignored_items INTEGER NOT NULL DEFAULT 0,
    invalid_items INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(user_id, idempotency_key)
);

