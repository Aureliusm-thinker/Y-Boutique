# 02 — Product Requirements Document (PRD)

| Field | Value |
|---|---|
| Product | Yvette — Modern Online Boutique |
| Version | 0.1 (draft) |
| Date | 2026-11-11 |
| Owner | Business Owner (TBC) |
| Related | [Project Plan](01-project-plan.md) · [Actors](03-actors-and-use-cases.md) · [AI/ML Strategy](04-ai-ml-strategy.md) · [Backlog](06-mvp-backlog.md) |

---

## 1. Problem Statement

Shoppers looking for distinctive, curated fashion online face two problems: large marketplaces are overwhelming and impersonal, while small boutiques often have clunky websites, poor mobile experiences and no personalisation. The boutique owner, meanwhile, needs a simple way to manage products, orders and customers without technical help.

## 2. Product Goals

1. **Delight** — a premium, editorial look and feel; fast on mobile.
2. **Convert** — remove friction from discovery to checkout.
3. **Retain** — accounts, wishlists, reviews, email marketing, and later AI personalisation bring customers back.
4. **Empower the owner** — an admin back-office that runs the business day-to-day.
5. **Learn** — capture the right data from day one so AI features can be added safely and effectively.

## 3. Target Users / Personas

| Persona | Description | Key needs |
|---|---|---|
| **Amara, 28 — Style-conscious professional** | Shops on her phone during commutes; wants unique pieces, not mass-market | Fast mobile browsing, outfit inspiration, easy returns |
| **Lindiwe, 41 — Busy repeat customer** | Knows the brand, buys gifts and seasonal items | Saved details, quick reorder, wishlist, size confidence |
| **Jordan, 22 — Browser/discoverer** | Arrives from Instagram/TikTok; price sensitive | Visual discovery, promotions, social proof (reviews) |
| **The Owner** | Runs the boutique; not technical | Simple product upload, order overview, sales insight |
| **Staff member** | Packs orders, updates stock, answers customers | Clear order queue, stock adjustments, customer lookup |

*(Personas are placeholders — refine with real customer insight in Phase 0.)*

## 4. Actors (summary)

Full detail, permissions and use cases are in [03 — Actors & Use Cases](03-actors-and-use-cases.md).

**Human:** Guest Visitor · Registered Customer · Store Owner (Super Admin) · Store Manager · Inventory/Merchandising Staff · Fulfilment Staff · Customer Support Agent · Marketing/Content Editor
**System/External:** Payment Gateway · Shipping/Courier Service · Email & SMS Service · AI Services (Recommendation Engine, AI Stylist Assistant, Forecasting) · Analytics Platform · Scheduler (background jobs)

## 5. Scope by Release

| Capability | MVP (Phase 1) | Phase 2 | Phase 3 (AI) | Later |
|---|:-:|:-:|:-:|:-:|
| Catalogue, collections, variants | ✅ | | | |
| Keyword search & filters | ✅ | | Semantic search | Visual search |
| Cart & checkout (guest + account) | ✅ | | | |
| Online payments & discount codes | ✅ | Gift cards | | |
| Customer accounts, wishlist, order history | ✅ | | | |
| Reviews & ratings | ✅ | Photo reviews | Review summaries | |
| Admin: products, inventory, orders, customers | ✅ | | | |
| Shipping & returns workflow | ✅ | | | |
| Transactional emails | ✅ | SMS / WhatsApp | | |
| Newsletter & abandoned-cart emails | | ✅ | Personalised content | |
| Related products | Manual | Rule-based | ML recommendations | |
| AI Stylist assistant | | | ✅ | Voice |
| Size & fit recommendation | Size guide | | ✅ | |
| AI product description generator (admin) | | | ✅ | |
| Demand forecasting / restock alerts | Low-stock threshold | | ✅ | |
| Loyalty programme | | | | ✅ |
| Multi-currency / multi-language | | | | ✅ |

## 6. Functional Requirements

Priority: **M** = Must (MVP) · **S** = Should · **C** = Could · **W** = Won't (this release)

### 6.1 Storefront & Discovery
| ID | Requirement | Priority |
|---|---|---|
| FR-SF-01 | Home page with hero banner(s), featured collections, new arrivals and best sellers, all editable from admin | M |
| FR-SF-02 | Collection and category pages with product grid, pagination/infinite scroll | M |
| FR-SF-03 | Filters (size, colour, price range, category, availability) and sorting (newest, price, popularity) | M |
| FR-SF-04 | Keyword search with autocomplete and typo tolerance | M |
| FR-SF-05 | Product detail page: image gallery with zoom, variants (size/colour), price, sale price, stock status, description, materials & care, size guide, reviews, related products | M |
| FR-SF-06 | "Complete the look" — manually curated product pairings | S |
| FR-SF-07 | Recently viewed products | S |
| FR-SF-08 | Content pages (About, Contact, FAQ, Shipping, Returns, Terms, Privacy) editable from admin | M |
| FR-SF-09 | Lookbook / editorial blog pages | C |

### 6.2 Cart & Checkout
| ID | Requirement | Priority |
|---|---|---|
| FR-CO-01 | Add/remove/update quantity in cart; cart persists across sessions (and merges on login) | M |
| FR-CO-02 | Guest checkout and logged-in checkout | M |
| FR-CO-03 | Address capture with validation; saved addresses for registered customers | M |
| FR-CO-04 | Shipping method selection with calculated rates; free-shipping threshold | M |
| FR-CO-05 | Discount/promo code application (percentage, fixed, free shipping; min spend; expiry; usage limits) | M |
| FR-CO-06 | Card payments via hosted payment provider; at least one local/alternative method (e.g., wallet, EFT, BNPL — TBC per market) | M |
| FR-CO-07 | Tax calculation and display per market rules | M |
| FR-CO-08 | Order confirmation page and email | M |
| FR-CO-09 | Stock is reserved/decremented atomically to prevent overselling | M |
| FR-CO-10 | Gift message / gift wrap option | C |

### 6.3 Customer Account
| ID | Requirement | Priority |
|---|---|---|
| FR-CA-01 | Register / log in with email + password; social login (Google/Apple) | M / S |
| FR-CA-02 | Password reset, email verification | M |
| FR-CA-03 | Profile, saved addresses, communication preferences | M |
| FR-CA-04 | Order history with status tracking and invoice download | M |
| FR-CA-05 | Wishlist (save for later), with back-in-stock notifications | M / S |
| FR-CA-06 | Request a return/exchange from order history | M |
| FR-CA-07 | Style profile (preferred sizes, styles, colours) — used later by AI | S |
| FR-CA-08 | Delete account & export my data (privacy rights) | M |

### 6.4 Reviews
| ID | Requirement | Priority |
|---|---|---|
| FR-RV-01 | Verified buyers can leave star rating + text review, including fit feedback (runs small / true / large) | M |
| FR-RV-02 | Reviews moderated by staff before publishing | M |
| FR-RV-03 | Photo reviews | C |

### 6.5 Admin Back-Office
| ID | Requirement | Priority |
|---|---|---|
| FR-AD-01 | Role-based access control (see Actors doc for permission matrix) | M |
| FR-AD-02 | Product management: create/edit/archive, variants, SKUs, pricing, sale pricing, images (drag-and-drop, auto-optimised), SEO fields, tags, collections | M |
| FR-AD-03 | Bulk product import/export (CSV) | S |
| FR-AD-04 | Inventory management: stock per variant, adjustments with reason, low-stock threshold alerts | M |
| FR-AD-05 | Order management: list/search/filter, order detail, status workflow (Pending → Paid → Processing → Shipped → Delivered / Cancelled / Refunded), notes, print packing slip, refunds (full/partial) | M |
| FR-AD-06 | Shipping: create shipment, enter/generate tracking number, notify customer | M |
| FR-AD-07 | Returns: approve/reject, receive, restock, refund | M |
| FR-AD-08 | Customer management: view profile, orders, notes; no access to raw card data | M |
| FR-AD-09 | Promotions: discount codes, automatic sales, scheduled start/end | M |
| FR-AD-10 | Content management: home page sections, banners, content pages, navigation menus | M |
| FR-AD-11 | Dashboard & reports: sales, orders, AOV, top products, conversion funnel, low stock | M |
| FR-AD-12 | Audit log of admin actions | S |
| FR-AD-13 | Review moderation queue | M |

### 6.6 Notifications
| ID | Requirement | Priority |
|---|---|---|
| FR-NT-01 | Transactional emails: account welcome, verification, password reset, order confirmation, shipped (with tracking), delivered, refund, return status | M |
| FR-NT-02 | Admin notifications: new order, low stock, new review, return request | M |
| FR-NT-03 | Marketing emails: newsletter sign-up, abandoned cart, back-in-stock | S (Phase 2) |
| FR-NT-04 | SMS/WhatsApp order updates | C |

### 6.7 Analytics & Data (foundation for AI)
| ID | Requirement | Priority |
|---|---|---|
| FR-DA-01 | Track standard e-commerce events (view item, search, add to cart, begin checkout, purchase, etc.) with consent | M |
| FR-DA-02 | Store first-party behavioural events in our own database/warehouse (not only third-party analytics) | M |
| FR-DA-03 | Cookie/consent banner; respect opt-outs in tracking and personalisation | M |
| FR-DA-04 | Structured product attributes (colour, pattern, material, fit, occasion, style tags) captured in admin | M |

### 6.8 AI / ML Features (Phase 3 — summarised; detail in [04 — AI/ML Strategy](04-ai-ml-strategy.md))
| ID | Requirement | Priority |
|---|---|---|
| FR-AI-01 | Personalised product recommendations (home, PDP, cart, email) | S |
| FR-AI-02 | Semantic / natural-language search ("flowy white dress for a beach wedding") | S |
| FR-AI-03 | AI Stylist chat assistant grounded on our live catalogue, able to add items to cart and hand off to a human | S |
| FR-AI-04 | Size & fit recommendation using customer history and review fit feedback | S |
| FR-AI-05 | AI-generated product descriptions, SEO metadata and tags in admin (human approves) | S |
| FR-AI-06 | Demand forecasting and restock suggestions for the owner | C |
| FR-AI-07 | Review summarisation on product pages | C |
| FR-AI-08 | Visual search ("upload a photo, find similar") | C (later) |
| FR-AI-09 | All AI features controllable via feature flags and measurable via A/B tests | M (for Phase 3) |

## 7. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Mobile LCP < 2.5s, INP < 200ms, CLS < 0.1 on key pages (p75); API p95 < 300ms |
| NFR-02 | Availability | 99.5% monthly uptime for storefront and checkout |
| NFR-03 | Scalability | Handle 10× normal traffic during promotions without degradation |
| NFR-04 | Security | OWASP Top 10 addressed; HTTPS everywhere; passwords hashed (Argon2/bcrypt); MFA for admin users; rate limiting on auth & checkout |
| NFR-05 | Payments | PCI-DSS SAQ-A scope — card data handled only by the payment provider's hosted fields |
| NFR-06 | Privacy | Compliant with applicable law (e.g., GDPR / POPIA — market TBC): consent, data minimisation, right to access/delete |
| NFR-07 | Accessibility | WCAG 2.2 AA |
| NFR-08 | SEO | Server-rendered pages, structured data (Product, Offer, Review), sitemap, clean URLs, canonical tags |
| NFR-09 | Responsiveness | Mobile-first; supports latest 2 versions of major browsers |
| NFR-10 | Observability | Centralised logs, error tracking, uptime monitoring, alerting |
| NFR-11 | Backup & Recovery | Daily DB backups, point-in-time recovery; RPO ≤ 24h, RTO ≤ 4h |
| NFR-12 | Maintainability | Automated tests (unit, integration, E2E for checkout), CI on every PR, IaC for infra |
| NFR-13 | Responsible AI | AI outputs grounded in our data, no invented products/prices; disclosure that the assistant is AI; human hand-off; no sensitive-attribute inference; cost and latency budgets |

## 8. Key User Journeys (MVP)

1. **Discover → Buy (guest):** Instagram link → product page → select size → add to cart → guest checkout → pay → confirmation email.
2. **Search → Filter → Buy (registered):** Log in → search "linen" → filter size M, under X → product → add to cart → saved address → pay.
3. **Wishlist → Return later:** Save items → receive back-in-stock / price drop email → purchase.
4. **Post-purchase:** Track order → receive item → leave review with fit feedback → request return if needed.
5. **Owner daily ops:** Log in → dashboard → process new orders → print packing slips → mark shipped with tracking → respond to returns → add new arrivals.

## 9. Success Metrics (KPIs)

| Metric | Initial target |
|---|---|
| Conversion rate (sessions → orders) | ≥ 2.5% |
| Average order value (AOV) | Baseline at launch, +10% after recommendations |
| Cart abandonment | < 70% |
| Returning customer rate | ≥ 25% |
| Return rate | < 15% (size & fit AI aims to reduce) |
| Page performance (mobile Lighthouse) | ≥ 90 |
| AI-attributed revenue (Phase 3) | ≥ 10% |
| Assistant resolution without human hand-off (Phase 3) | ≥ 70% |

## 10. Assumptions & Constraints
- Single brand, single store, single currency and language at launch.
- Product range is fashion/accessories with size and colour variants (to confirm).
- The owner/staff will supply product photos and copy.
- Budget favours managed/cloud services over self-hosted infrastructure.
- See [Open Questions](07-open-questions-and-decisions.md) for unresolved items.

## 11. Acceptance / Sign-off

| Role | Name | Date | Signature |
|---|---|---|---|
| Business Owner | | | |
| Tech Lead | | | |
| Design Lead | | | |
