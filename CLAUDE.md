# gallary4j — PhotoVault

Enterprise photo/video gallery SaaS for photographers (freelancers to studios).

## Tech Stack

| Layer | Technology |
|---|---|
| API Backend | Spring Boot 3.2 (Java 21) |
| Admin Dashboard | Next.js 14 + TypeScript (`photovault-admin`, port 3000) |
| Client Gallery | Next.js + SSG/ISR (`photovault-gallery`, port 3001) |
| Database | PostgreSQL 16 |
| Cache | Hazelcast IMDG (distributed) |
| Storage | Cloudflare R2 + CDN (local fallback for dev) |
| Message Queue | Apache Kafka |
| Orchestration | Kubernetes — see `k8s/` |
| Monitoring | Prometheus + Grafana |

## Key Architecture Decisions

- **Storage**: `STORAGE_TYPE=local` for dev, `r2` or `s3` for prod. Set via env vars.
- **Kafka** replaces RabbitMQ for media processing events.
- **Hazelcast** handles distributed caching (albums, media DTOs). Config in `HazelcastConfig.java`.
- **Presigned URLs**: iOS/Android upload directly to R2; backend only stores metadata.
- **Partitioned tables**: `albums`, `media`, and `analytics_events` are partitioned by date. Managed by `pg_partman`.

## Project Layout

```
photovault-backend/   Spring Boot API (port 8080)
photovault-admin/     Admin dashboard (Next.js, port 3000)
photovault-gallery/   Public client gallery (Next.js, port 3001)
k8s/                  Kubernetes manifests
init.sql              PostgreSQL schema
docker-compose.yml    Local infrastructure (Postgres + Kafka)
QUICKSTART.md         Step-by-step local setup
```

## Running Locally

See `QUICKSTART.md` for full steps. Short version:

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Backend (port 8080)
cd photovault-backend && ./mvnw spring-boot:run

# 3. Admin (port 3000) — new terminal
cd photovault-admin && npm install && npm run dev

# 4. Gallery (port 3001) — new terminal
cd photovault-gallery && npm install && npm run dev
```

## Database Schema (tables)

Full DDL in `init.sql` and `photovault-backend/src/main/resources/db/migration/`.

| Table | Purpose |
|---|---|
| `photographers` | Users/studios, subscription info, storage limits |
| `albums` | Galleries (partitioned by `created_at`) |
| `media` | Photos & videos with EXIF, processing status (partitioned by `uploaded_at`) |
| `selections` | Client favorites/picks |
| `analytics_events` | View/download/share events (partitioned by day) |
| `upload_sessions` | Chunked upload tracking |

## Subscription Tiers

`trial` → `starter` ($9) → `pro` ($29) → `studio` ($99) → `enterprise` (custom)

Stored in `photographers.plan`. Limits enforced in `AlbumService` and `StorageService`.

## Success Metrics

- Upload success rate: >99.5%
- Image processing: <10s, video: <60s
- Gallery load: <2s
- Uptime: 99.9%
