# 05 — Architecture & Tech Stack (Proposed)

| Field | Value |
|---|---|
| Version | 0.1 (draft — to be confirmed in Phase 0) |
| Date | 2026-11-11 |

---

## 1. Build Approach Options

| Option | Description | Pros | Cons |
|---|---|---|---|
| A. Hosted platform (Shopify etc.) + custom theme | Use a SaaS commerce platform | Fastest, cheapest to run, proven checkout | Monthly + transaction fees, limited control, AI features constrained to apps/APIs |
| **B. Headless open-source commerce engine + custom frontend** *(recommended)* | e.g., Medusa (Node/TS) for commerce logic + Next.js storefront & admin | Full control & ownership, modern UX, clean integration of a custom AI service, no platform lock-in | More build & hosting effort than A |
| C. Fully custom build | Write all commerce logic ourselves | Maximum flexibility | Slowest, highest risk (payments, inventory, tax are hard) |

**Decision (2026-11-11): custom headless build — Java (Spring Boot) API + Next.js frontend** (a variant of Option B/C chosen by the owner). Commerce logic is written in a modular Java monolith, so payments, tax and shipping use established providers rather than being built from scratch. See [Decision Log](07-open-questions-and-decisions.md), D-01.

## 2. Proposed Stack

| Layer | Choice | Why |
|---|---|---|
| Storefront | **Next.js 16 (React 19, TypeScript)**, Tailwind CSS 4, Cache Components | SSR + partial prerendering for SEO & speed, PWA-capable |
| Admin | Next.js `/admin` area (same app, role-protected) | One frontend codebase; admin APIs secured server-side by role |
| Commerce API | **Java 21 + Spring Boot 4** — modular monolith (catalog, inventory, cart, checkout, order, customer, review, promotion, content, events, ai) | Mature, strongly-typed, excellent for transactional business logic |
| Persistence | Spring Data JPA (Hibernate), **Flyway** migrations | Versioned, reviewable schema changes |
| Database | **PostgreSQL 17** (+ **pgvector** for embeddings) | Reliable, relational, vector search without another DB |
| Cache / queues | **Redis** | Sessions, caching, background job queues |
| Search | Meilisearch/Typesense (keyword) → hybrid with pgvector (Phase 3) | Fast typo-tolerant search |
| AI service | **Python + FastAPI** | Best ML ecosystem (pandas, scikit-learn, PyTorch, LightGBM) |
| LLM | Claude API (e.g., `claude-sonnet-5-5` for the stylist, `claude-haiku-5-5` for high-volume/cheap tasks) | Strong tool use & reasoning; tiered cost |
| Media | Cloudinary or S3 + image CDN | Auto-optimised responsive images |
| Payments | Stripe / Paystack / PayFast / Adyen (by market) | Hosted fields → PCI SAQ-A |
| Email | Resend / Postmark (transactional), Klaviyo/Brevo (marketing) | Deliverability, templates |
| Analytics | PostHog (events, funnels, feature flags, A/B tests) + own event table | One tool for analytics + flags + experiments |
| Hosting | Vercel (storefront) + managed container platform (Railway/Render/Fly/AWS ECS) for APIs + managed Postgres | Low ops overhead |
| Observability | Sentry (errors), uptime monitor, structured logs | |
| CI/CD | GitHub + GitHub Actions | Tests, lint, preview deployments |
| IaC | Terraform (when infra grows beyond managed defaults) | Reproducible environments |

## 3. High-Level Architecture

```
               ┌──────────────┐       ┌──────────────┐
  Shoppers ───►│  Storefront  │       │    Admin     │◄─── Staff / Owner
               │  (Next.js)   │       │  (Back-office)│
               └──────┬───────┘       └──────┬───────┘
                      │   HTTPS (REST/GraphQL)│
                      ▼                       ▼
               ┌──────────────────────────────────────┐
               │     Commerce API (Java / Spring Boot) │
               │ products · carts · orders · customers │
               │ promotions · inventory · RBAC · events│
               └───┬──────────┬───────────┬───────────┘
                   │          │           │
          ┌────────▼──┐  ┌────▼────┐  ┌───▼──────────┐       ┌──────────────────┐
          │PostgreSQL │  │  Redis  │  │ Job workers  │──────►│ Email/SMS, Courier│
          │ +pgvector │  │ (queue) │  │ (scheduler)  │       └──────────────────┘
          └────▲──────┘  └─────────┘  └──────────────┘
               │                                              ┌──────────────────┐
               │            ┌──────────────────────┐ webhooks │ Payment Gateway  │
               └────────────┤  AI Service (Python) │          └──────────────────┘
                            │ recs·search·stylist  │──► LLM API / embedding models
                            │ size·content·forecast│
                            └──────────────────────┘
```

## 4. Core Domain Model (outline)

| Entity | Key fields / notes |
|---|---|
| Product | id, title, slug, description, status (draft/active/archived), brand, category, attributes (JSON + normalised columns), SEO fields |
| ProductVariant | id, product_id, sku, size, colour, price, compare_at_price, barcode, weight |
| InventoryLevel | variant_id, location_id, on_hand, reserved, low_stock_threshold |
| ProductImage | product_id, variant_id?, url, alt_text, position |
| Collection / Category | id, title, slug, rules (manual or automatic), image |
| Customer | id, email, name, phone, password_hash / social ids, marketing_consent, style_profile (JSON) |
| Address | customer_id, type, lines, city, region, postal_code, country |
| Cart / CartItem | cart_id, customer_id?, anonymous_id, items, discount_codes, expires_at |
| Order / OrderItem | order_number, customer_id?, email, status, payment_status, fulfilment_status, totals, tax, shipping, discounts |
| Payment / Refund | order_id, provider, provider_ref, amount, status |
| Shipment | order_id, carrier, tracking_number, status, events |
| ReturnRequest / ReturnItem | order_item_id, reason, size_issue, status, refund_id |
| Review | product_id, customer_id, order_item_id, rating, text, fit_feedback, status |
| Wishlist / WishlistItem | customer_id, variant_id |
| Promotion / DiscountCode | type, value, conditions, starts_at, ends_at, usage_limit |
| StaffUser / Role / Permission | RBAC per [Actors doc](03-actors-and-use-cases.md) |
| Event | event_id, name, anonymous_id, user_id, session_id, properties (JSON), consent_state, ts |
| AuditLog | actor_id, action, entity, before/after, ts |
| *(Phase 3)* ProductEmbedding, Recommendation, ChatSession/ChatMessage, Forecast | AI layer |

## 5. Cross-Cutting Concerns
- **Security:** OWASP ASVS L2 as a guide; secrets in a vault/env manager; admin MFA; rate limiting; dependency scanning (Dependabot); CSP headers.
- **Idempotency:** payment & courier webhooks verified by signature and processed idempotently.
- **Inventory correctness:** reservations with expiry; atomic decrement in DB transaction on payment success.
- **Testing:** unit (domain logic), integration (API + DB), E2E (Playwright) for browse → checkout → admin fulfil.
- **Environments:** local (Docker Compose) · staging · production; seed data for dev.

## 6. Repository Structure (proposed monorepo)

```
yvette/
├── apps/
│   ├── api/               # Java 21 / Spring Boot 4 commerce API (Maven)
│   │   └── src/main/java/com/yvette/api/<module>/   # one package per domain module
│   └── web/               # Next.js storefront (+ /admin area later)
├── services/
│   └── ai/                # Python FastAPI AI service (Phase 3)
├── infra/                 # docker-compose (Postgres+pgvector, Redis, Mailpit); IaC later
├── docs/                  # these documents
└── .github/workflows/     # CI
```
