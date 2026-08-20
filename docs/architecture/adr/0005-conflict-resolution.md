# ADR 0005: Explicit conflict resolution

- Status: accepted
- Tasks and preferences: optimistic version checks; a stale mutation returns
  HTTP 409 with current server state. Tombstones prevent deleted tasks from
  reappearing after offline synchronization.
- Sessions: append-only events plus a strict state machine. Duplicate commands
  return the already-produced state. Competing commands serialize on the user
  and session records; the first valid transition wins.
- IDs: the API accepts safe client-generated identifiers so local imports and
  retries are naturally idempotent.

