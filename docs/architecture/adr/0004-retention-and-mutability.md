# ADR 0004: Immutable history and recoverable deletion

- Status: accepted
- Decision: completed and abandoned sessions cannot be edited or deleted through
  the product API. Tasks use soft deletion and sessions retain a task-title
  snapshot. Accounts enter a 30-day deletion window before permanent erasure;
  export is available before deletion. Security logs may be retained for up to
  90 days without task titles or token material.
- Stopwatch: remains device-local until user research supports remote history.

