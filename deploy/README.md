# Deploy

Minimal production stack for a single 4GB VPS (~$5/mo).

## Layout

- `docker-compose.prod.yml` — Postgres + Spring Boot backend + Caddy reverse proxy
- `Caddyfile` — auto-TLS config for the API domain
- `.env.example` — secrets template

The Next.js admin and gallery apps are **not** in this compose stack — deploy them to Vercel or Cloudflare Pages (both free).

## First-time VPS setup

```bash
# 1. Provision a Hetzner CX22 (2 vCPU / 4GB), Ubuntu 24.04
# 2. Install docker
curl -fsSL https://get.docker.com | sh

# 3. Clone and configure
git clone <this-repo> /opt/photovault
cd /opt/photovault/deploy
cp .env.example .env && $EDITOR .env

# 4. Point DNS: api.yourdomain.com A -> VPS IP
# 5. Launch
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Caddy fetches a Let's Encrypt cert on first request to `https://api.yourdomain.com`.

## Updates

```bash
cd /opt/photovault
git pull
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build backend
```

## Backups

Nightly `pg_dump` to R2 is handled inside the backend (`S3BackupService`) when `BACKUP_ENABLED=true`. Verify via `docker compose logs backend | grep -i backup`.

## Frontends

Deploy `photovault-admin` and `photovault-gallery` to Vercel:

```bash
cd photovault-admin && vercel --prod
cd photovault-gallery && vercel --prod
```

Set env var `NEXT_PUBLIC_API_URL=https://api.yourdomain.com` in the Vercel project.
