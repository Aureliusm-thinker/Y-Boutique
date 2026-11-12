# Yvette — Modern Online Boutique

A modern, curated online boutique (fashion & lifestyle) with a planned AI/ML layer for personalization, styling assistance and smarter operations.

> **Status:** Phase 1 — Sprint 1 (foundations & catalogue management) complete (2026-11-12)
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

# 3. Start the storefront  →  http://localhost:3000  (back office: http://localhost:3000/admin)
cd apps/web
cp .env.example .env.local
npm install
npm run dev
```

### Back office (local dev)

The `dev` profile creates an owner account on first start:

| Email | Password |
|---|---|
| `owner@yvette.local` | `yvette-dev-owner` |

These exist only in the `dev` profile. In any other environment the API refuses to start without
`JWT_SECRET`, and the first owner is created from `YVETTE_BOOTSTRAP_OWNER_EMAIL` / `_PASSWORD`.

### Tests

```bash
cd apps/api && ./mvnw test     # integration tests use Testcontainers (Docker must be running)
cd apps/web && npm run lint && npm run build
```

## What works today

| Area | Status |
|---|---|
| Storefront: home, shop with category filter, product page with size picker, consent banner + `product_view` tracking | ✅ |
| Staff sign-in (signed token in an httpOnly cookie), roles per the permission matrix, login rate limiting | ✅ |
| Back office: dashboard, products (create, edit, variants, images, publish/unpublish/archive), categories, low-stock inventory, staff accounts | ✅ |
| Publishing rules: category, material, description, image and a priced variant are required | ✅ |
| Stock changes recorded with reason, note, person and resulting level | ✅ |
| Image uploads (JPEG/PNG/WebP, checked by file content), stored locally in dev | ✅ |
| Two-factor login for staff, error tracking (Sentry), CSV import/export | ⏳ next |
| Cart, checkout, payments, customer accounts, reviews, promotions | ⏳ per [MVP backlog](docs/06-mvp-backlog.md) |
| AI features | ⏳ Phase 3 ([AI/ML strategy](docs/04-ai-ml-strategy.md)) |

## API endpoints

| Method | Path | Access |
|---|---|---|
| GET | `/api/v1/catalog/categories`, `/products`, `/products/{slug}` | public |
| POST | `/api/v1/events` | public (sent only with consent) |
| POST | `/api/v1/auth/staff/login` | public (rate limited) |
| GET | `/api/v1/admin/me` | any staff |
| GET/POST/PUT | `/api/v1/admin/products`, `/products/{id}` | read: owner, manager, inventory, marketing · write: owner, manager, inventory |
| POST | `/api/v1/admin/products/{id}/publish` · `/unpublish` · `/archive` | owner, manager, inventory |
| GET/POST/PUT | `/api/v1/admin/categories` | as products |
| POST | `/api/v1/admin/inventory/adjustments` | owner, manager, inventory |
| GET | `/api/v1/admin/inventory/low-stock`, `/variants/{id}/adjustments` | as product read |
| POST | `/api/v1/admin/media` | owner, manager, inventory |
| GET/POST/PUT | `/api/v1/admin/staff` | owner only |
| GET | `/media/**`, `/actuator/health` | public |

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
