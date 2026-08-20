# ADR 0001: Accounts augment local-first use

- Status: accepted
- Decision: Vesta remains usable without an account. Anonymous data remains on
  the device. Creating or signing into an account enables cloud persistence and
  synchronization. A deliberate, idempotent import moves existing local data
  into the account; local data is retained until the server confirms the import.
- Consequence: the client needs a local outbox and must never discard pending
  operations during logout. Anonymous data is not recoverable from the server.

