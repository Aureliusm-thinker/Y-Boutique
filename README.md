# Yvette — Modern Online Boutique

A modern, curated online boutique (fashion & lifestyle) for Kenya, with a planned AI/ML layer for personalization,
styling assistance and smarter operations.

> **Status:** Phase 1 MVP — Sprints 1–5 built and tested (2026-11-13). Prices in Kenyan shillings (KES); payment by
> M-Pesa or card. "Yvette" is the working project name; the final brand name is an open question.

## Repository layout

```
apps/
  api/        Java 21 · Spring Boot 4 commerce API (Maven, Flyway, PostgreSQL)
  web/        Next.js 16 storefront + back office (TypeScript, Tailwind CSS 4)
services/
  ai/         Python AI service — placeholder for Phase 3
infra/        docker-compose for local Postgres (+pgvector), Redis, Mailpit
docs/         Plan, PRD, actors, AI strategy, architecture, backlog, decisions
```

## Run it locally

Prerequisites: Java 21, Node 20+, Docker.

```bash
# 1. Start local services (Postgres on host port 55432, Redis 6379, Mailpit on 11025 / UI http://localhost:18025)
docker compose -f infra/docker-compose.yml up -d

# 2. Start the API with demo data  →  http://localhost:8080
cd apps/api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Windows: mvnw.cmd ...

# 3. Start the shop  →  http://localhost:3000  (back office: http://localhost:3000/admin)
cd apps/web
cp .env.example .env.local
npm install
npm run dev
```

All emails (order confirmations, password resets, shipping updates…) land in **Mailpit**: http://localhost:18025.

### Logins (local dev only)

| Who | Where | Login |
|---|---|---|
| Store owner | http://localhost:3000/admin | `owner@yvette.local` / `yvette-dev-owner` |
| Shopper | http://localhost:3000/account/register | create any account (verify via Mailpit) |

Demo discount codes: `KARIBU10` (10% off), `FREESHIP` (free delivery over KSh 5,000).

**Test payments (dev):** no real money moves. At checkout choose M-Pesa and press *Approve payment* on the next page,
or choose card and use `4242 4242 4242 4242` (or `4000 0000 0000 0002` to see a decline).

These logins and test payments exist only in the `dev` profile. In any other environment the API refuses to start
without `JWT_SECRET`, test payments are off, staff must use two-step sign-in, and the first owner is created from
`YVETTE_BOOTSTRAP_OWNER_EMAIL` / `_PASSWORD`.

### Tests

```bash
cd apps/api && ./mvnw test     # 43 integration tests, real Postgres via Testcontainers (Docker must be running)
cd apps/web && npm run lint && npx tsc --noEmit && npm run build
```

## What works

| Area | What's there |
|---|---|
| **Shop** | Editable home page and announcement bar · categories · filters (size, colour, price, in stock) and sorting in the URL · search with autocomplete and typo tolerance ("dreses" finds dresses) · product page with zoomable gallery, colour/size picker, "only N left", size guide, reviews with fit summary, "complete the look", recently viewed · wishlist · back-in-stock emails |
| **Bag & checkout** | Bag kept across visits and devices · discount codes and automatic sales · Kenyan delivery options (Nairobi, countrywide, same-day, collection) with free-delivery thresholds · guest checkout · M-Pesa (STK push) and card · stock held for 30 minutes while paying — two people can never buy the last item · confirmation page, email and printable invoice |
| **Customer accounts** | Register, email verification (links past guest orders), sign-in, password reset · profile, style preferences, saved addresses · order history and tracking · returns with reasons (incl. size) · reviews for delivered items · download or delete my data · guest order lookup |
| **Back office** | Dashboard (sales, orders, average order, top products, funnel, return reasons, work queue) · orders queue, shipping with tracking, delivery, refunds with restock, cancellations, notes, packing slips · customers with notes · returns workflow · review moderation · products, categories, stock with audit trail, "complete the look" · discount codes and scheduled sales · pages, home page and announcement with preview · staff and roles · two-step sign-in (authenticator app) |
| **SEO & privacy** | Server-rendered pages, `sitemap.xml`, `robots.txt`, product structured data with price, stock and ratings · consent banner; behavioural events stored only with consent · legal pages editable by staff |
| **Not yet** | Sentry/uptime monitoring (needs an account) · Google/Apple sign-in (needs app credentials) · CSV import/export · AI features (Phase 3, [AI/ML strategy](docs/04-ai-ml-strategy.md)) |

## Going live: settings to provide

Set these on the API (environment variables). Everything else has sensible defaults in `application.yml`.

| Setting | What it's for |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL 17 (with pgvector and pg_trgm) |
| `JWT_SECRET` | 32+ random characters; signs staff and customer sessions |
| `YVETTE_BOOTSTRAP_OWNER_EMAIL` / `_PASSWORD` | First owner account (remove after first start) |
| `SITE_URL`, `CORS_ALLOWED_ORIGINS` | The shop's public address, e.g. `https://yvette.co.ke` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | Email delivery (e.g. Amazon SES, Postmark, Mailgun) |
| `STAFF_NOTIFICATIONS_TO` | Inbox for new-order alerts |
| `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_SHORTCODE`, `MPESA_PASSKEY` | Safaricom Daraja app ("Lipa na M-Pesa Online") |
| `MPESA_BASE_URL` | `https://api.safaricom.co.ke` for live (sandbox is the default) |
| `MPESA_TRANSACTION_TYPE` | `CustomerPayBillOnline` (paybill) or `CustomerBuyGoodsOnline` (till) |
| `MPESA_CALLBACK_SECRET` | Long random string; `MPESA_CALLBACK_URL` = `https://<api>/api/v1/payments/mpesa/callback/<secret>` |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Card payments via Stripe Checkout; webhook URL `https://<api>/api/v1/payments/stripe/webhook` (events `checkout.session.completed`, `checkout.session.expired`) |
| `MEDIA_DIR`, `MEDIA_PUBLIC_BASE_URL` | Where product photos are stored and served from |

On the web app: `API_URL`, `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_SITE_URL`, `MEDIA_PUBLIC_BASE_URL` (see `apps/web/.env.example`).

M-Pesa refunds are recorded in the back office but sent manually from the M-Pesa business account (Daraja's reversal
API needs separate approval from Safaricom); card refunds go back through Stripe automatically.

## Project documents

| # | Document | Purpose |
|---|----------|---------|
| 01 | [Project Plan](docs/01-project-plan.md) | Vision, scope, phases, milestones, team, risks |
| 02 | [Product Requirements (PRD)](docs/02-prd.md) | What we are building and why; functional & non-functional requirements |
| 03 | [Actors & Use Cases](docs/03-actors-and-use-cases.md) | Every human and system actor, their goals, permissions and use cases |
| 04 | [AI / ML Strategy](docs/04-ai-ml-strategy.md) | AI features, data we must collect from day one, rollout plan |
| 05 | [Architecture & Tech Stack](docs/05-architecture.md) | System design, stack, data model outline |
| 06 | [MVP Backlog (User Stories)](docs/06-mvp-backlog.md) | Epics and user stories, with what's done |
| 07 | [Open Questions & Decisions](docs/07-open-questions-and-decisions.md) | Decisions needed from the owner, and a decision log |
