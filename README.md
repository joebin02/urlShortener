# Shortly — URL Shortener with Analytics

A full-stack URL shortener built to explore production service fundamentals: redirect latency, cache-aside reads, atomic distributed rate limiting, database indexing, asynchronous click analytics, containerized development, and CI.

**Stack:** Java 21 · Spring Boot 3 · PostgreSQL · Redis · Angular 18 · Docker Compose · k6 · GitHub Actions

> Performance numbers belong to a measured environment. Run the included k6 script before publishing any throughput or latency claim; this repository does not claim a completed benchmark.

## Features

- Seven-character URL-safe codes generated with `SecureRandom`; a database unique constraint handles collisions safely.
- Redirect lookups use a Redis cache-aside flow with a 24-hour TTL and PostgreSQL as the source of truth.
- Redis Lua token bucket enforces per-IP limits atomically across API instances. Defaults: 30-token burst, 1 token/second refill.
- Clicks are recorded asynchronously so the redirect response does not wait for analytics writes. PostgreSQL stores event history and daily aggregates.
- Angular dashboard creates links, searches recent links, copies share URLs, and charts click counts for the past 7 days.
- Flyway-managed schema, Docker Compose local stack, and GitHub Actions build workflow.

## Run locally

Prerequisites: Docker Desktop (or Docker Engine) and Docker Compose.

```bash
docker compose up --build
```

Open the dashboard at [http://localhost:4200](http://localhost:4200). The API is available at [http://localhost:8080](http://localhost:8080). Stop with `Ctrl+C`; remove the database volume with `docker compose down -v`.

To use Angular’s dev server while keeping the API and its dependencies in containers:

```bash
docker compose up -d postgres redis api
cd frontend && npm install && npm start
```

## API

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/api/links` | Create; JSON body `{"url":"https://example.com"}`; returns `201` and code/short URL |
| `GET` | `/api/links` | List the 100 most recently created links |
| `GET` | `/{code}` | `302` redirect and asynchronously enqueue a click record |
| `GET` | `/api/links/{code}/stats` | Total clicks and daily counts over the past 7 days |

Rate limited requests receive `429`. URLs must use `http` or `https`. Set `APP_BASE_URL` to the public redirect origin before deployment.

## Architecture and key decisions

```mermaid
flowchart LR
  Browser[Angular dashboard / visitor] -->|create, list, stats| API[Spring Boot API]
  Browser -->|short URL redirect| API
  API -->|cache-aside GET / SET TTL| Redis[(Redis)]
  API -->|atomic Lua token bucket| Redis
  API -->|link metadata| PG[(PostgreSQL)]
  API -. async click event .-> Worker[Spring async executor]
  Worker -->|event history + counter| PG
```

The redirect path checks Redis first. A miss reads PostgreSQL and populates Redis; subsequent reads avoid a relational query. The database unique index remains the final authority for random-code collisions. The token bucket script reads elapsed time, replenishes fractional tokens up to capacity, spends one token, and updates state in one Redis operation, avoiding races between API replicas. Click writes run after the redirect has been selected; the event table supports historical analytics while the link counter supports quick summaries.

The async click handler currently writes to PostgreSQL directly through Spring's executor. That is intentionally simpler for a portfolio demo; if event volume requires durable buffering and backpressure, a message broker (for example Kafka) is the next step. Analytics are eventually consistent by design.

## Load testing

Install [k6](https://k6.io/docs/get-started/installation/) and create a link first. Then run:

```bash
RATE_LIMIT_CAPACITY=2000 RATE_LIMIT_REFILL_PER_SECOND=2000 docker compose up -d --force-recreate api
BASE_URL=http://localhost:8080 SHORT_CODE=YOURCODE k6 run load-test/redirects.js
```

The script targets 1,000 successful redirects/second for 60 seconds and reports error rate and p95 latency. The Compose default rate limit is intentionally lower, so raise the limit as shown before the run; 429 responses fail the check. Adjust VUs and thresholds to available hardware. Measure with a warm cache and cold cache separately, document machine/database topology, and repeat runs. Do not quote a benchmark result until you have captured and can reproduce it.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | local PostgreSQL | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `shortly` / `shortly` | Database credentials |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` | `localhost:6379` | Redis endpoint |
| `APP_BASE_URL` | `http://localhost:8080` | Host used in generated short URLs |
| `RATE_LIMIT_CAPACITY` | `30` | Maximum token bucket burst |
| `RATE_LIMIT_REFILL_PER_SECOND` | `1` | Token refill rate per identity |

## Security and production follow-ups

This is an educational starter, not a hardened public deployment. Production work should include trusted-proxy configuration for client IPs, authentication/tenant ownership, abuse and phishing defenses, deletion/expiry, Redis outage policy, database connection pooling/tuning, structured metrics/tracing, and a durable analytics queue. Add TLS and managed secrets; never deploy the Compose sample credentials publicly.

## Explain it in an interview

Start with the request path: creation validates the URL, allocates a random code, and persists it behind a unique constraint. On redirect, the service checks Redis and falls back to PostgreSQL on a cache miss. The token bucket uses a Redis Lua script so check, refill, and decrement are atomic across replicas. It returns the redirect without waiting for analytics; a background executor records a click event and updates summary data. Then discuss tradeoffs: cache TTL/staleness, collision probability and unique-index retries, eventual consistency, and when you'd add a durable queue or partition event storage.

## Deploy on Oracle Cloud Always Free

The project can run as four Docker containers on one small Ubuntu VM: Angular/Nginx, Spring Boot, PostgreSQL, and Redis. The VM must be in an Always Free eligible shape and stay within Oracle's current Always Free resource limits. Oracle notes that free compute capacity may temporarily be unavailable in some regions. See [Oracle Always Free resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm).

### 1. Create the VM

In the Oracle Cloud Console, create a Compute instance in your home region. Choose an Always Free eligible Ampere A1 shape and an Ubuntu 24.04 ARM64 image. Attach a public IPv4 address and your SSH public key. A 1 OCPU / 6 GB configuration is a reasonable starting point when available.

In the subnet security list (or a Network Security Group), allow inbound TCP 22 only from your own public IP address and TCP 80 from `0.0.0.0/0`. Do not open ports 8080, 5432, or 6379. The app publishes only the web entry point; the API binds to loopback and PostgreSQL/Redis stay inside Docker's private network.

### 2. Install Docker

Copy the contents of `deploy/oracle-cloud/install-docker-ubuntu.sh` into the instance's cloud-init user-data field when creating the VM. Docker officially supports Ubuntu on ARM64 and provides the package repository used by this script: [Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/).

If the VM already exists, SSH into it and run that script with `sudo` instead.

### 3. Configure and start the app

SSH to the VM as `ubuntu`, then run:

```bash
git clone https://github.com/joebin02/urlShortener.git
cd urlShortener
cp .env.oracle.example .env
nano .env
```

Replace `REPLACE_WITH_PUBLIC_IP` in `APP_BASE_URL` with the VM's public IPv4 address. Set `SHORTLY_DB_PASSWORD` to a long random value. Then start the stack:

```bash
sudo docker compose up -d --build
sudo docker compose ps
```

Open `http://YOUR_PUBLIC_IP` in a browser. The generated short links will use that public IP and route through Nginx. The app uses HTTP for an IP-only demo; use a domain and configure HTTPS before treating it as a public service. Keep the VM and all resources within the Always Free limits, and monitor Oracle Cloud billing and usage.

### Update the demo

After a change is pushed to GitHub, SSH into the VM and run:

```bash
./deploy/oracle-cloud/deploy.sh
```

### Stop or remove the demo

Stop containers while keeping the data volume:

```bash
sudo docker compose down
```

To delete the app's database volume as well:

```bash
sudo docker compose down -v
```

Stopping containers does not stop or delete the Oracle VM. Stop or terminate the VM in the Oracle Console when you no longer want it running; check Oracle's current billing rules for storage and any other attached resources.
