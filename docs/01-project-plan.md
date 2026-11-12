# 01 — Project Plan

| Field | Value |
|---|---|
| Project | Yvette — Modern Online Boutique |
| Version | 0.1 (draft) |
| Date | 2026-11-11 |
| Status | Draft — awaiting owner review |

---

## 1. Vision

Build a modern, mobile-first online boutique that feels like a personal shopping experience rather than a catalogue: curated collections, beautiful product presentation, a fast and frictionless checkout, and — over time — an AI layer that learns each shopper's style and helps them find, combine and buy pieces they love.

**Vision statement:** *"Your personal boutique — curated for you, styled with you."*

## 2. Objectives

| # | Objective | Measure of success (first 12 months after launch) |
|---|-----------|---------------------------------------------------|
| O1 | Launch a reliable, beautiful storefront | MVP live; ≥ 99.5% uptime; Lighthouse performance ≥ 90 on mobile |
| O2 | Make buying effortless | Checkout conversion ≥ 2.5% of sessions; cart abandonment < 70% |
| O3 | Give the owner full control without developers | Owner can add products, run promotions and fulfil orders unaided |
| O4 | Build customer loyalty | ≥ 25% of orders from returning customers |
| O5 | Differentiate with AI | AI features drive ≥ 10% of revenue (recommendation/assistant-attributed) by end of Phase 3 |

*(Targets are initial assumptions to be validated with the owner.)*

## 3. Scope

### In scope (overall project)
- Customer storefront (web, mobile-responsive; PWA-capable)
- Product catalogue: collections, categories, variants (size/colour), rich media
- Search, filtering and sorting
- Cart, checkout, online payments, discount codes
- Customer accounts: profile, addresses, order history, wishlist
- Order management & fulfilment workflow, shipping and returns
- Admin back-office: products, inventory, orders, customers, promotions, content, reports
- Transactional emails/SMS
- Reviews and ratings
- Analytics & event tracking (foundation for AI)
- AI/ML features (phased — see [AI/ML Strategy](04-ai-ml-strategy.md))

### Out of scope (for now)
- Native iOS/Android apps (PWA first; native later if justified)
- Multi-vendor marketplace (third-party sellers)
- Physical point-of-sale integration
- Wholesale / B2B pricing
- Multi-currency & multi-language (revisit after launch — see Open Questions)

## 4. Phases & Milestones

```
Phase 0  Discovery & Design      ██░░░░░░░░░░░░░░░░░░░░  ~3 weeks
Phase 1  MVP Build               ░░████████░░░░░░░░░░░░  ~10 weeks
Phase 2  Launch & Growth         ░░░░░░░░░░████░░░░░░░░  ~6 weeks
Phase 3  AI Layer                ░░░░░░░░░░░░░░██████░░  ~10 weeks (rolling)
Phase 4  Scale & Optimise        ░░░░░░░░░░░░░░░░░░░░██  ongoing
```

### Phase 0 — Discovery & Design (≈3 weeks)
- Finalise PRD, actors, brand identity, target market & legal entity
- Choose payment provider, shipping carriers, hosting
- Information architecture, wireframes, UI design system, high-fidelity mockups (key screens)
- Technical architecture sign-off; repository, CI/CD and environments set up
- **Milestone M0:** PRD + designs approved, dev environment ready

### Phase 1 — MVP Build (≈10 weeks, 5 two-week sprints)
| Sprint | Focus |
|---|---|
| S1 | Foundations: auth, data model, admin product management, image pipeline |
| S2 | Storefront: home, collections, product listing, product detail, search/filter |
| S3 | Cart, checkout, payments, discount codes, order confirmation emails |
| S4 | Customer accounts, wishlist, order history; admin order management & fulfilment |
| S5 | Shipping/returns, reviews, analytics events, SEO, hardening, UAT |
- **Milestone M1:** MVP feature-complete and passing UAT

### Phase 2 — Launch & Growth (≈6 weeks)
- Soft launch (friends & family) → public launch
- Monitoring, bug fixing, performance tuning
- Newsletter, abandoned-cart emails, basic "related products" (rule-based)
- Start accumulating clean behavioural data for ML
- **Milestone M2:** Public launch; first 100 orders

### Phase 3 — AI Layer (≈10 weeks, incremental releases)
- 3a: Semantic search + personalised recommendations
- 3b: AI Stylist chat assistant ("Ask Yvette")
- 3c: Size & fit recommendation; AI-assisted product descriptions for admin
- 3d: Demand forecasting & low-stock prediction for the owner
- **Milestone M3:** AI features live behind feature flags, measured via A/B tests

### Phase 4 — Scale & Optimise (ongoing)
- Loyalty programme, visual search ("shop the look" from a photo), multi-currency, native app evaluation

## 5. Team & Roles (project roles, not system actors)

| Role | Responsibility | Notes |
|---|---|---|
| Product Owner / Business Owner | Vision, priorities, sign-off, budget | The boutique owner |
| Project/Delivery Lead | Plan, sprints, risks, communication | Can be combined with Tech Lead in a small team |
| UX/UI Designer | Brand application, wireframes, design system | Heavy in Phase 0 |
| Full-stack Developer(s) | Storefront, admin, APIs | 1–2 |
| ML / AI Engineer | Recommendations, assistant, forecasting | Joins from Phase 2 |
| QA | Test plans, UAT support, automation | Part-time |
| Content / Photography | Product photos, copy | Critical for a boutique — often underestimated |

## 6. Delivery Approach
- **Agile/Scrum**, 2-week sprints, sprint review with the owner at the end of each sprint.
- **Definition of Done:** code reviewed, tests passing, deployed to staging, acceptance criteria met, documentation updated.
- **Environments:** Local → Staging (auto-deploy from `main`) → Production (tagged releases).
- **Tooling (proposed):** Git + GitHub, GitHub Actions CI, project board (Linear/Jira/GitHub Projects), Figma for design.

## 7. Key Risks

| # | Risk | Impact | Likelihood | Mitigation |
|---|------|--------|------------|------------|
| R1 | Scope creep (especially AI features creeping into MVP) | High | High | Strict MVP backlog; AI gated to Phase 3 |
| R2 | Poor product photography/content quality | High | Medium | Plan content production in Phase 0; photo guidelines |
| R3 | Not enough data for ML to be useful | Medium | High | Track events from day one; start with rule-based/LLM approaches that need little data |
| R4 | Payment/security/compliance issues (PCI, GDPR/POPIA etc.) | High | Low | Use hosted payment fields (no card data touches our servers); privacy-by-design |
| R5 | AI assistant gives wrong/inappropriate answers | Medium | Medium | Grounded answers from catalogue only, guardrails, human hand-off, monitoring |
| R6 | Performance on mobile with heavy imagery | Medium | Medium | Image CDN, responsive images, performance budget in CI |
| R7 | Single developer dependency | Medium | Medium | Documentation, code review, infrastructure as code |

## 8. Budget Drivers (to estimate once Open Questions are answered)
- Design & development effort (largest cost)
- Hosting/infrastructure, CDN, database
- Payment fees (% per transaction)
- Email/SMS provider
- AI usage costs (LLM API tokens, vector database, model hosting)
- Photography & content
- Domain, SSL, legal (terms, privacy policy)

## 9. Communication
- Weekly status update (progress, next steps, blockers, decisions needed)
- Sprint review/demo every 2 weeks
- Decisions recorded in [07 — Open Questions & Decisions](07-open-questions-and-decisions.md)
