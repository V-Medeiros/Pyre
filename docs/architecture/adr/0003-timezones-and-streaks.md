# ADR 0003: Capture timezone per focus session

- Status: accepted
- Decision: users store an IANA timezone. It is copied to a session when that
  session starts and remains fixed even if the user travels. The completion day
  is derived from the server completion instant in that captured timezone.
  Timezone changes affect only future sessions.
- Streak: completed session dates are the source of truth. Abandoned sessions do
  not count. Multiple completions on one date count once. `longestEver` is
  derived rather than independently mutated.

