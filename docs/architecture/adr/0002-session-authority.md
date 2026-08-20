# ADR 0002: Server-authoritative session state

- Status: accepted
- Decision: the UI renders a local wall-clock countdown, but the server owns
  state transitions and timestamps for signed-in users. Only one RUNNING or
  PAUSED session may exist per user. Commands are idempotent and entities use
  optimistic versions. A scheduled reconciler completes expired sessions.
- Offline: an origin device may continue locally and enqueue transition intents.
  Replayed events are accepted only when they form a valid transition; server
  state wins when another device has already advanced the session.
- Consequence: clients reconcile on startup, visibility changes, and reconnect.

