# 06 — MVP Backlog (Epics & User Stories)

| Field | Value |
|---|---|
| Version | 0.1 (draft) |
| Date | 2026-11-11 |

Priority uses MoSCoW. Estimates (story points) to be added during sprint planning. Story IDs map to PRD requirement IDs where noted.

---

> **Status (2026-11-13):** ✅ done · 🟡 partly done · ⏳ not started. Sprints 1–5 are built and tested (43 API tests, 74 browser checks).
> Still open: E0-4 (Sentry/uptime — needs an account), E1-5 (CSV import/export), E4-2 (Google/Apple sign-in — needs app credentials).
> Kenya-specific: prices in KES; M-Pesa + card payments; Kenyan delivery options.

## Epic E0 — Foundations (Sprint 1)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E0-1 | As a developer, I want a monorepo with CI, linting and tests so that quality is enforced from day one | 🟡 PRs run lint, type-check, tests; main auto-deploys to staging *(CI done; no staging environment yet — needs hosting, D-03)* | M |
| E0-2 | As a developer, I want local dev via Docker Compose (Postgres, Redis, API, storefront) | ✅ `docker compose up` gives a working stack with seed data | M |
| E0-3 | As the Owner, I want staff accounts with roles so that people only see what they need | ✅ Roles per permission matrix; MFA for admin users | M |
| E0-4 | As the business, I want error tracking and uptime monitoring | ⏳ Sentry + uptime alerts configured for staging & prod | M |

## Epic E1 — Catalogue Management (Sprint 1)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E1-1 | As Inventory Staff, I want to create a product with variants (size/colour), prices and images | ✅ Product saved as draft/active; variants have unique SKUs; images auto-optimised (FR-AD-02) | M |
| E1-2 | As Inventory Staff, I want to set structured attributes (colour, material, fit, occasion, style tags) | ✅ Required attributes validated before publishing (FR-DA-04) | M |
| E1-3 | As Inventory Staff, I want to organise products into collections and categories | ✅ Manual & rule-based collections | M |
| E1-4 | As Inventory Staff, I want to adjust stock with a reason | ✅ Adjustment history recorded; low-stock alert at threshold (FR-AD-04) | M |
| E1-5 | As Inventory Staff, I want to import/export products via CSV | ⏳ Validates rows, reports errors | S |

## Epic E2 — Storefront & Discovery (Sprint 2)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E2-1 | As a shopper, I want an inspiring home page | 🟡 Sections editable in admin; LCP < 2.5s mobile (FR-SF-01) *(editable ✅; LCP to be measured on real hosting)* | M |
| E2-2 | As a shopper, I want to browse collections with filters and sorting | ✅ Filters: size, colour, price, availability; URL reflects filters (FR-SF-02/03) | M |
| E2-3 | As a shopper, I want to search with autocomplete and typo tolerance | ✅ "dres" finds dresses; zero-result page suggests alternatives (FR-SF-04) | M |
| E2-4 | As a shopper, I want a rich product page | ✅ Gallery + zoom, variant selection updates price/stock, size guide, reviews (FR-SF-05) | M |
| E2-5 | As a shopper, I want to see "complete the look" items | ✅ Manually curated pairs shown on PDP (FR-SF-06) | S |
| E2-6 | As a shopper, I want recently viewed items | ✅ Stored per device/account (FR-SF-07) | S |
| E2-7 | As the business, I want SEO-ready pages | ✅ SSR, structured data, sitemap, meta tags (NFR-08) | M |

## Epic E3 — Cart & Checkout (Sprint 3)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E3-1 | As a shopper, I want a persistent cart | ✅ Survives refresh/return visit; merges on login (FR-CO-01) | M |
| E3-2 | As a shopper, I want to check out as a guest | ✅ No account required; option to create account after purchase (FR-CO-02) | M |
| E3-3 | As a shopper, I want to choose shipping and see costs before paying | ✅ Rates shown; free-shipping threshold applied (FR-CO-04) | M |
| E3-4 | As a shopper, I want to apply a discount code | ✅ Validated server-side; clear errors (FR-CO-05) | M |
| E3-5 | As a shopper, I want to pay securely by card and a local method | 🟡 Hosted fields; 3DS supported; declines handled (FR-CO-06) *(M-Pesa + Stripe Checkout built and tested with test stand-ins; needs live Daraja/Stripe keys to test end to end)* | M |
| E3-6 | As a shopper, I want a confirmation page and email | ✅ Sent within 1 minute of payment (FR-CO-08) | M |
| E3-7 | As the business, I never want to oversell | ✅ Concurrent checkout test for last unit passes (FR-CO-09) | M |

## Epic E4 — Customer Accounts (Sprint 4)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E4-1 | As a shopper, I want to register and log in | ✅ Email verification; password reset; rate limiting (FR-CA-01/02) | M |
| E4-2 | As a customer, I want to log in with Google/Apple | ⏳ OAuth works; accounts linked by verified email | S |
| E4-3 | As a customer, I want to manage profile and addresses | ✅ CRUD; default address (FR-CA-03) | M |
| E4-4 | As a customer, I want to see my orders and track them | ✅ Status timeline + tracking link; invoice PDF (FR-CA-04) | M |
| E4-5 | As a customer, I want a wishlist | ✅ Add/remove; move to cart (FR-CA-05) | M |
| E4-6 | As a customer, I want back-in-stock alerts | ✅ Email sent when variant restocked | S |
| E4-7 | As a customer, I want to export or delete my data | ✅ Self-service; completed within legal timeframe (FR-CA-08) | M |
| E4-8 | As a customer, I want to save my style preferences | ✅ Optional sizes/styles/colours stored (FR-CA-07) | S |

## Epic E5 — Order Management & Fulfilment (Sprint 4)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E5-1 | As Fulfilment Staff, I want an order queue | ✅ Filter by status; oldest first; new-order notification (FR-AD-05) | M |
| E5-2 | As Fulfilment Staff, I want to print packing slips | ✅ PDF per order / batch | M |
| E5-3 | As Fulfilment Staff, I want to mark orders shipped with tracking | ✅ Customer gets shipped email with link (FR-AD-06) | M |
| E5-4 | As a Manager, I want to issue full/partial refunds | ✅ Processed via gateway; order & stock updated; customer notified | M |
| E5-5 | As Support, I want to look up customers and orders and add notes | ✅ Search by email, name, order number (FR-AD-08) | M |

## Epic E6 — Returns & Reviews (Sprint 5)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E6-1 | As a customer, I want to request a return from my order history | ✅ Reason incl. size issue captured; window enforced (FR-CA-06) | M |
| E6-2 | As Support/Fulfilment, I want to approve, receive and restock returns | ✅ Status workflow; refund triggered (FR-AD-07) | M |
| E6-3 | As a verified buyer, I want to leave a review with fit feedback | ✅ Only after delivery; moderation queue (FR-RV-01/02) | M |

## Epic E7 — Promotions, Content & Reports (Sprint 5)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E7-1 | As Marketing, I want to create discount codes and scheduled sales | ✅ Types, limits, schedule (FR-AD-09) | M |
| E7-2 | As Marketing, I want to edit home page, banners, menus and content pages | ✅ Preview before publish (FR-AD-10) | M |
| E7-3 | As the Owner, I want a dashboard of sales and key metrics | ✅ Sales, orders, AOV, top products, low stock, funnel (FR-AD-11) | M |

## Epic E8 — Data & Compliance (Sprints 2–5, ongoing)
| ID | Story | Acceptance criteria | Pri |
|---|---|---|---|
| E8-1 | As the business, I want first-party e-commerce events stored | ✅ All events in AI/ML Strategy §3.1 captured & validated (FR-DA-01/02) | M |
| E8-2 | As a shopper, I want to control cookies/tracking consent | ✅ Consent banner; tracking respects choice (FR-DA-03) | M |
| E8-3 | As the business, I want legal pages and policies | ✅ Terms, Privacy, Returns, Shipping, Cookies published | M |
| E8-4 | As the business, I want the site to be accessible | 🟡 Automated a11y checks in CI; manual audit of key flows (NFR-07) *(axe checks pass on 7 key pages in the local browser tests; not yet in CI; no manual audit yet)* | M |

## Post-MVP (parked)
- Newsletter & abandoned-cart flows (Phase 2)
- Rule-based related products (Phase 2)
- Gift cards, gift wrap, photo reviews
- All AI features AI-1 … AI-9 (Phase 3+, see [AI/ML Strategy](04-ai-ml-strategy.md))
- Loyalty programme, multi-currency, native apps
