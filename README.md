# Yvette — Modern Online Boutique

A modern, curated online boutique (fashion & lifestyle) with a planned AI/ML layer for personalization, styling assistance and smarter operations.

> **Status:** Phase 0/1 — planning done, scaffold in place (2026-11-11)
> "Yvette" is the working project name; the final brand name is an open question.

## Repository layout

```
apps/
  api/        Java 21 · Spring Boot 4 commerce API (Maven, Flyway, PostgreSQL)
  web/        Next.js 16 storefront (TypeScript, Tailwind CSS 4)
services/
  ai/         Python AI service — placeholder for Phase 3
infra/        docker-compose for local Postgres (+pgvector), Redis, Mailpit
docs/         Plan, PRD, actors, AI strategy, architecture, backlog, decisions
```

## Run it locally

Prerequisites: Java 21, Node 20+, Docker.

```bash
# 1. Start local services (Postgres on host port 55432, Redis 6379, Mailpit UI on 18025)
docker compose -f infra/docker-compose.yml up -d

# 2. Start the API with demo catalogue data  →  http://localhost:8080
cd apps/api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Windows: mvnw.cmd ...

# 3. Start the storefront  →  http://localhost:3000
cd apps/web
cp .env.example .env.local
npm install
npm run dev
```

### Tests

```bash
cd apps/api && ./mvnw test     # integration tests use Testcontainers (Docker must be running)
cd apps/web && npm run lint && npm run build
```

## What works today

| Area | Status |
|---|---|
| Catalogue API: categories, product list (paged, by category), product detail with per-size stock | ✅ |
| Behavioural event capture (`POST /api/v1/events`) — the data foundation for AI | ✅ |
| Security baseline: public storefront endpoints, everything else requires auth, admin routes role-gated, CORS | ✅ (no login yet) |
| Storefront: home, shop with category filter, product page with size picker, 404, consent banner + `product_view` tracking | ✅ |
| Cart, checkout, payments, accounts, admin UI, reviews, promotions | ⏳ per [MVP backlog](docs/06-mvp-backlog.md) |
| AI features | ⏳ Phase 3 ([AI/ML strategy](docs/04-ai-ml-strategy.md)) |

## API endpoints

| Method | Path | Access |
|---|---|---|
| GET | `/api/v1/catalog/categories` | public |
| GET | `/api/v1/catalog/products?category=&page=&size=` | public |
| GET | `/api/v1/catalog/products/{slug}` | public |
| POST | `/api/v1/events` | public (sent only with consent) |
| GET | `/actuator/health` | public |
| * | `/api/v1/admin/**` | staff roles (not implemented yet) |

## Project documents

| # | Document | Purpose |
|---|----------|---------|
| 01 | [Project Plan](docs/01-project-plan.md) | Vision, scope, phases, milestones, team, risks |
| 02 | [Product Requirements (PRD)](docs/02-prd.md) | What we are building and why; functional & non-functional requirements |
| 03 | [Actors & Use Cases](docs/03-actors-and-use-cases.md) | Every human and system actor, their goals, permissions and use cases |
| 04 | [AI / ML Strategy](docs/04-ai-ml-strategy.md) | AI features, data we must collect from day one, rollout plan |
| 05 | [Architecture & Tech Stack](docs/05-architecture.md) | System design, stack, data model outline |
| 06 | [MVP Backlog (User Stories)](docs/06-mvp-backlog.md) | Epics and prioritized user stories for the first release |
| 07 | [Open Questions & Decisions](docs/07-open-questions-and-decisions.md) | Decisions needed from the owner, and a decision log |
