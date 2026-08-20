# Vesta API

Optional cloud continuity for the local-first Vesta client.

## Run locally

From the repository root:

```powershell
docker compose up --build
```

The API listens on `http://localhost:8080`, Swagger UI on
`http://localhost:8080/swagger-ui`, and PostgreSQL on port `5432`.

Without Docker, point the environment variables in `.env.example` to a local
PostgreSQL instance and run:

```powershell
cd vesta-api
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

## Verify

```powershell
.\mvnw.cmd -B verify
```

Integration tests use Testcontainers and are skipped only when Docker is not
available. CI runs them with PostgreSQL 17.

## API groups

| Prefix | Responsibility |
| --- | --- |
| `/api/v1/auth` | Registration, access, refresh rotation, verification, recovery |
| `/api/v1/me` | Profile, export, account deletion |
| `/api/v1/tasks` | Versioned task lifecycle and soft deletion |
| `/api/v1/preferences` | Focus, sound, and theme preferences |
| `/api/v1/focus-sessions` | Server-authoritative focus state machine |
| `/api/v1/history`, `/streak`, `/stats` | Derived progress views |
| `/api/v1/imports` | Idempotent localStorage migration |
| `/api/v1/sync` | Bootstrap and incremental server changes |

All endpoints except the public authentication operations require an access
token in `Authorization: Bearer <token>`. Refresh uses a rotating HttpOnly
cookie plus the `vesta_csrf` cookie value in `X-CSRF-Token`.

## Production requirements

- Set every `VESTA_*` secret explicitly; never use local defaults.
- Terminate TLS at the platform load balancer or reverse proxy.
- Use managed PostgreSQL with point-in-time recovery.
- Configure SMTP and monitor failed delivery.
- Restrict `VESTA_ALLOWED_ORIGINS` to exact frontend origins.
- Serve frontend and API on same-site HTTPS domains so Strict refresh cookies work.
- Keep metrics endpoints private at the network edge.

The frontend cloud entry point can be released gradually with
`VITE_CLOUD_ENABLED=false|true` without removing anonymous local use.
