# Vesta API runbook

## Health

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- Metrics: `/actuator/prometheus` (private network only)

Alert on sustained 5xx responses, p95 latency, authentication rejection spikes,
database pool saturation, migration failures, and overdue RUNNING sessions.

## Deployment

1. Build and verify the immutable image in CI.
2. Run Flyway against staging and execute smoke tests.
3. Confirm a restorable database snapshot.
4. Deploy with readiness gating.
5. Run registration, refresh, task, and focus-session smoke tests.
6. Promote the same image digest to production after approval.

Flyway migrations must be backward compatible for at least one application
release. Roll back application code first. Repair data with a reviewed forward
migration rather than automatically reversing a production migration.

## Backup and restoration

Enable daily backups and point-in-time recovery. Quarterly, restore into an
isolated database, run Flyway validation, compare row counts, and exercise one
user export. A backup is valid only after a restore succeeds.

## Suspected token exposure

1. Rotate `VESTA_JWT_SECRET` through a controlled deployment.
2. Revoke all refresh sessions.
3. Inspect logs by correlation ID without copying tokens.
4. Notify affected users when required by the incident policy.

