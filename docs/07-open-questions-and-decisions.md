# 07 — Open Questions & Decision Log

| Field | Value |
|---|---|
| Version | 0.1 |
| Date | 2026-11-11 |

---

## 1. Open Questions for the Business Owner

Answers to these will refine scope, cost and timeline.

### Business & brand
| # | Question | Why it matters |
|---|---|---|
| Q1 | What is the final brand name? Is "Yvette" the brand? Do you have a logo, colours, fonts? | Design system, domain, legal |
| Q2 | What exactly will you sell? (women's clothing, menswear, accessories, shoes, jewellery, beauty, home?) | Product attributes, size logic, AI training |
| Q3 | Who is your target customer (age, style, price point)? | UX, tone, marketing |
| Q4 | Roughly how many products/SKUs at launch, and in year one? | Catalogue tooling, search, hosting |
| Q5 | Do you hold your own stock, use dropshipping, or a mix? | Inventory & fulfilment design |
| Q6 | Do you also have a physical shop? | POS/inventory sync |

### Market & operations
| # | Question | Why it matters |
|---|---|---|
| Q7 | Which country/countries will you sell to at launch? Which currency? | Payment provider, tax, privacy law (GDPR / POPIA / etc.), couriers |
| Q8 | Which payment methods do your customers expect (cards, mobile money, EFT, BNPL, wallets)? | Payment integration |
| Q9 | Which courier(s) will you use? Do you need click-and-collect? | Shipping integration |
| Q10 | What is your returns policy (window, who pays return shipping, exchanges)? | Returns workflow |
| Q11 | How many staff will use the admin, and in which roles? | RBAC, licences |

### Budget & timeline
| # | Question | Why it matters |
|---|---|---|
| Q12 | Target launch date? Any key date (season, event) to hit? | Plan & scope trade-offs |
| Q13 | Budget range for build and monthly running costs? | Build approach (D-01), hosting, AI usage |
| Q14 | Who is on the team (in-house devs, agency, freelancers)? | Plan, tech choices |
| Q15 | Who will produce product photos and copy? | Content readiness is a launch blocker |

### AI
| # | Question | Why it matters |
|---|---|---|
| Q16 | Which AI feature excites you most (stylist chat, recommendations, size advice, content generation, forecasting)? | Phase 3 prioritisation |
| Q17 | What tone/personality should the AI Stylist have? Should it be named "Yvette"? | Brand voice, prompt design |
| Q18 | Monthly budget ceiling for AI usage costs? | Model choice, rate limits |

## 2. Decisions Required

| ID | Decision | Options | Recommendation | Status |
|---|---|---|---|---|
| D-01 | Build approach | Hosted SaaS · Headless open-source · Fully custom | — | **Decided:** custom Java (Spring Boot) API + Next.js |
| D-02 | Payment provider | Stripe · Paystack · PayFast · Adyen · other | Depends on Q7/Q8 | **Built:** M-Pesa (Daraja STK push) + card via Stripe Checkout; swap card provider if preferred (e.g. Paystack, Pesapal) |
| D-03 | Hosting | Vercel + Railway/Render · AWS · Azure · GCP | Vercel + managed containers + managed Postgres | Open |
| D-04 | Analytics / feature flags | PostHog · GA4 + LaunchDarkly · other | PostHog (+ GA4 optional) | Open |
| D-05 | LLM provider for AI features | Claude · others | Claude (tiered models) | Open |
| D-06 | Search engine | Meilisearch · Typesense · Algolia · Postgres FTS | Meilisearch, then hybrid with pgvector | **MVP:** Postgres with pg_trgm (typo-tolerant); revisit at ~5,000 products |
| D-07 | Project management tool | Linear · Jira · GitHub Projects | GitHub Projects (simple, co-located with code) | Open |

## 3. Decision Log

| Date | ID | Decision | Decided by | Notes |
|---|---|---|---|---|
| 2026-11-11 | — | Project initiated; AI/ML to be included as a later phase with data captured from MVP | Owner | Initial brief |
| 2026-11-11 | D-01 | Backend in **Java** (Spring Boot 4, Java 21); frontend in **Next.js** (TypeScript). AI service stays Python (Phase 3). | Owner | Owner's language preference; scaffold created in `apps/api` and `apps/web` |
| 2026-11-13 | — | Market is **Kenya**: prices in **KES**, M-Pesa as the local payment method, Kenyan delivery options and counties | Owner | "in kenya shillings please" |
| 2026-11-13 | D-02 | Payments built for M-Pesa (Safaricom Daraja) and card (Stripe Checkout), each switched on by its keys; test stand-ins in dev | Dev team | Needs the owner's Daraja and Stripe accounts to go live |
| 2026-11-13 | — | Staff two-step sign-in (authenticator app) required outside dev | Dev team | Sprint 1 E0-3 |
| 2026-11-13 | — | Returns window 14 days from delivery; Support can refund up to KSh 5,000 per refund (configurable) | Dev team | Defaults — owner to confirm |
