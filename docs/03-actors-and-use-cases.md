# 03 — Actors & Use Cases

| Field | Value |
|---|---|
| Version | 0.1 (draft) |
| Date | 2026-11-11 |

An **actor** is anyone or anything outside the system that interacts with it to achieve a goal. We distinguish **primary human actors** (they start interactions), **back-office human actors** (they run the business), and **system/external actors** (other software we integrate with).

---

## 1. Actor Overview

```
                         ┌──────────────────────────────────────────┐
  Guest Visitor ───────► │                                          │ ◄──── Payment Gateway
  Registered Customer ─► │                                          │ ◄──── Shipping / Courier Service
                         │          YVETTE BOUTIQUE PLATFORM        │ ◄──── Email & SMS Service
  Store Owner ─────────► │   Storefront · Admin · APIs · AI Layer   │ ◄──── AI Services (LLM, Recs, Forecast)
  Store Manager ───────► │                                          │ ◄──── Analytics Platform
  Inventory Staff ─────► │                                          │ ◄──── Scheduler (background jobs)
  Fulfilment Staff ────► │                                          │
  Support Agent ───────► │                                          │
  Marketing Editor ────► │                                          │
                         └──────────────────────────────────────────┘
```

## 2. Human Actors

### A1. Guest Visitor
- **Description:** Anyone browsing the store without logging in.
- **Goals:** Discover products, get inspiration, buy quickly without creating an account.
- **Can:** Browse, search, filter, view products & reviews, use cart, guest checkout, subscribe to newsletter, chat with AI Stylist (Phase 3), track an order via order number + email.
- **Cannot:** Write reviews, use wishlist (beyond the session), view order history, save addresses.

### A2. Registered Customer
- **Description:** A shopper with an account (specialisation of Guest — inherits everything a guest can do).
- **Goals:** Faster repeat purchases, track orders, save favourites, get personalised suggestions.
- **Can (in addition):** Manage profile & addresses, view order history & invoices, wishlist, request returns, write reviews (verified purchase), set style profile & communication preferences, export/delete own data, receive personalised recommendations.

### A3. Store Owner (Super Admin)
- **Description:** The business owner. Full control.
- **Goals:** Grow sales, see business performance, control everything.
- **Can:** Everything in admin, including managing staff accounts & roles, store settings, payment/shipping configuration, viewing all reports, AI feature flags and AI settings.

### A4. Store Manager
- **Description:** Trusted senior staff running day-to-day operations.
- **Goals:** Keep the store running smoothly.
- **Can:** Products, inventory, orders, refunds, returns, customers, promotions, content, reports. **Cannot** manage store settings, payment configuration or Owner account.

### A5. Inventory / Merchandising Staff
- **Description:** Adds and maintains products and stock.
- **Goals:** Accurate, attractive product listings; correct stock levels.
- **Can:** Create/edit products, upload images, set attributes & collections, adjust stock, use AI description generator (Phase 3), view restock suggestions (Phase 3).

### A6. Fulfilment Staff
- **Description:** Picks, packs and ships orders; processes returned items.
- **Goals:** Ship orders correctly and on time.
- **Can:** View order queue, print packing slips, mark orders as processing/shipped, add tracking numbers, receive returns & restock.
- **Cannot:** Issue refunds, edit prices, see customer data beyond what is needed to ship.

### A7. Customer Support Agent
- **Description:** Answers customer questions and resolves issues.
- **Goals:** Resolve issues quickly and kindly.
- **Can:** Look up customers & orders, add order notes, approve returns, trigger resend of emails, moderate reviews, take over AI Stylist conversations (human hand-off, Phase 3). Refunds up to a configured limit (TBC).

### A8. Marketing / Content Editor
- **Description:** Owns the look, campaigns and content.
- **Goals:** Drive traffic and conversion with campaigns and content.
- **Can:** Manage home page, banners, collections' presentation, content pages, blog/lookbook, discount codes, newsletter; view marketing reports.

> In a small boutique one person may hold several roles (e.g., the Owner does everything at first). The system still models them as separate **roles** so permissions are clean as the team grows.

## 3. System / External Actors

| ID | Actor | Role | Interaction |
|---|---|---|---|
| S1 | **Payment Gateway** (e.g., Stripe / Paystack / PayFast / Adyen — TBC by market) | Takes payments, refunds, fraud screening | We create payment intents; it sends webhooks (payment succeeded/failed, refund, dispute) |
| S2 | **Shipping / Courier Service** | Rates, labels, tracking | We request rates & labels; it sends tracking status updates |
| S3 | **Email & SMS Service** (e.g., Resend/Postmark/SendGrid, Twilio) | Delivers notifications & marketing | We send templated messages; it reports bounces/unsubscribes |
| S4 | **AI Services** | Intelligence layer (Phase 3) | See sub-actors below |
| S4.1 | ↳ Recommendation Engine | Personalised & similar products | Reads events + catalogue; returns ranked product lists |
| S4.2 | ↳ AI Stylist Assistant (LLM) | Conversational shopping help | Uses tools: search catalogue, check stock, get size advice, add to cart, hand off to human |
| S4.3 | ↳ Search/Embedding Service | Semantic search | Embeds products and queries; returns matches |
| S4.4 | ↳ Forecasting Service | Demand & restock prediction | Reads sales/inventory history nightly; writes suggestions |
| S4.5 | ↳ Content Generation | Product descriptions, SEO, tags, review summaries | Called from admin; output requires human approval |
| S5 | **Analytics Platform** (e.g., GA4 / PostHog) | Behaviour analytics & experiments | Receives events (consent-dependent) |
| S6 | **Scheduler / Background Jobs** | Time-based automation | Releases expired stock reservations, sends abandoned-cart emails, starts/ends promotions, runs nightly ML jobs, back-in-stock alerts |
| S7 | **Image CDN / Media Service** (e.g., Cloudinary) | Image storage, optimisation, transforms | Admin uploads; storefront requests resized variants |
| S8 | **Identity Provider** (Google / Apple sign-in) | Social login | OAuth/OIDC |

## 4. Permission Matrix (Admin)

| Capability | Owner | Manager | Inventory | Fulfilment | Support | Marketing |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Store settings, payments, shipping config | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Manage staff & roles | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Products (create/edit/archive) | ✅ | ✅ | ✅ | ❌ | ❌ | 👁 |
| Pricing | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Inventory adjustments | ✅ | ✅ | ✅ | Restock returns only | ❌ | ❌ |
| View orders | ✅ | ✅ | 👁 | ✅ | ✅ | 👁 (aggregate) |
| Fulfil / ship orders | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| Refunds | ✅ | ✅ | ❌ | ❌ | Up to limit | ❌ |
| Returns approval | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| Customers (view) | ✅ | ✅ | ❌ | Shipping info only | ✅ | Segments only |
| Review moderation | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ |
| Promotions & discount codes | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| Content (home, pages, blog) | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| Reports | ✅ All | ✅ All | Inventory | Fulfilment | Support | Marketing |
| AI feature flags & settings | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| AI chat hand-off (take over) | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| Audit log | ✅ | 👁 | ❌ | ❌ | ❌ | ❌ |

✅ full · 👁 read-only · ❌ none

## 5. Use Case Catalogue

### Shopper use cases
| ID | Use case | Primary actor | Supporting actors |
|---|---|---|---|
| UC-01 | Browse collections & categories | Guest / Customer | Image CDN |
| UC-02 | Search & filter products | Guest / Customer | Search Service |
| UC-03 | View product details & reviews | Guest / Customer | — |
| UC-04 | Manage cart | Guest / Customer | Scheduler (reservation expiry) |
| UC-05 | Checkout & pay | Guest / Customer | Payment Gateway, Shipping, Email |
| UC-06 | Apply discount code | Guest / Customer | — |
| UC-07 | Register / log in / reset password | Guest | Email, Identity Provider |
| UC-08 | Manage profile, addresses, preferences | Customer | — |
| UC-09 | View order history & track order | Customer (Guest via lookup) | Shipping |
| UC-10 | Manage wishlist & back-in-stock alerts | Customer | Scheduler, Email |
| UC-11 | Request return / exchange | Customer | Email |
| UC-12 | Write review with fit feedback | Customer (verified buyer) | — |
| UC-13 | Subscribe / unsubscribe newsletter | Guest / Customer | Email |
| UC-14 | Export / delete my data | Customer | — |
| UC-15 | Chat with AI Stylist *(Phase 3)* | Guest / Customer | AI Stylist, Support Agent |
| UC-16 | Receive personalised recommendations *(Phase 3)* | Customer (and Guest by session) | Recommendation Engine |
| UC-17 | Get size recommendation *(Phase 3)* | Guest / Customer | AI Services |

### Back-office use cases
| ID | Use case | Primary actor | Supporting actors |
|---|---|---|---|
| UC-20 | Manage products & variants | Inventory Staff | Image CDN, Content Generation *(P3)* |
| UC-21 | Adjust inventory | Inventory Staff | — |
| UC-22 | Process & fulfil order | Fulfilment Staff | Shipping, Email |
| UC-23 | Issue refund | Manager / Support | Payment Gateway, Email |
| UC-24 | Process return | Support / Fulfilment | Payment Gateway, Email |
| UC-25 | Manage customers | Support / Manager | — |
| UC-26 | Moderate reviews | Support / Marketing | — |
| UC-27 | Create promotion / discount code | Marketing | Scheduler |
| UC-28 | Manage content & home page | Marketing | Image CDN |
| UC-29 | View dashboard & reports | Owner / Manager | Analytics |
| UC-30 | Manage staff & roles | Owner | Email |
| UC-31 | Configure store, payments, shipping | Owner | Payment Gateway, Shipping |
| UC-32 | Take over AI chat conversation *(Phase 3)* | Support Agent | AI Stylist |
| UC-33 | Review restock suggestions *(Phase 3)* | Owner / Inventory | Forecasting Service |
| UC-34 | Configure AI features & flags *(Phase 3)* | Owner | AI Services |

### System use cases
| ID | Use case | Primary actor |
|---|---|---|
| UC-40 | Process payment webhook (success/failure/refund/dispute) | Payment Gateway |
| UC-41 | Update shipment tracking status | Shipping Service |
| UC-42 | Release expired cart reservations | Scheduler |
| UC-43 | Send abandoned-cart & back-in-stock emails | Scheduler |
| UC-44 | Retrain / refresh ML models and embeddings | Scheduler + AI Services |

## 6. Detailed Use Cases (key flows)

### UC-05 — Checkout & Pay
| | |
|---|---|
| **Primary actor** | Guest Visitor or Registered Customer |
| **Supporting** | Payment Gateway, Shipping Service, Email Service |
| **Preconditions** | Cart contains ≥ 1 in-stock item |
| **Trigger** | Shopper clicks "Checkout" |
| **Main flow** | 1. System shows checkout (email/login option for guests). 2. Shopper enters/selects shipping address. 3. System fetches shipping options & rates. 4. Shopper selects shipping, optionally applies discount code. 5. System shows order summary with taxes & totals. 6. Shopper enters payment details in the gateway's secure fields. 7. Gateway authorises payment. 8. System creates order (status *Paid*), decrements stock, clears cart. 9. System shows confirmation and sends confirmation email. 10. Admin is notified of new order. |
| **Alternative flows** | 4a. Invalid/expired code → error message, totals unchanged. 7a. Payment declined → error, shopper may retry or change method; no order created. 7b. 3-D Secure challenge → shopper completes challenge then continue at 7. 8a. Item went out of stock during checkout → shopper notified, cart updated, payment not captured. |
| **Postconditions** | Order exists with status *Paid*; stock updated; customer and admin notified. |
| **Business rules** | Prices & discounts re-validated server-side at step 8; order confirmation idempotent against duplicate webhooks. |

### UC-22 — Process & Fulfil Order
| | |
|---|---|
| **Primary actor** | Fulfilment Staff |
| **Preconditions** | Order status *Paid* |
| **Main flow** | 1. Staff opens order queue (oldest first). 2. Opens order, prints packing slip. 3. Marks *Processing*, picks & packs. 4. Creates shipment (label via courier or manual tracking number). 5. Marks *Shipped* → customer receives shipped email with tracking. 6. Courier webhook later sets *Delivered*. |
| **Alternative flows** | 3a. Item missing/damaged → staff flags issue; Manager/Support contacts customer, partial refund or cancel. |
| **Postconditions** | Order *Shipped*/*Delivered*; customer informed. |

### UC-11 / UC-24 — Request & Process Return
| | |
|---|---|
| **Primary actors** | Registered Customer → Support Agent → Fulfilment Staff |
| **Preconditions** | Order delivered and within return window (e.g., 14/30 days — TBC) |
| **Main flow** | 1. Customer selects items and reason (incl. "too small/too large") from order history. 2. System creates return request (*Requested*). 3. Support approves → customer receives return instructions/label. 4. Fulfilment receives item, inspects, marks *Received* and restocks if resellable. 5. Support/Manager issues refund (or exchange). 6. Customer notified. |
| **Alternative flows** | 3a. Rejected (outside window/final sale) → customer notified with reason. |
| **Data note** | Return reasons feed the Size & Fit model (Phase 3). |

### UC-15 — Chat with AI Stylist (Phase 3)
| | |
|---|---|
| **Primary actor** | Guest or Registered Customer |
| **Supporting** | AI Stylist Assistant, Recommendation Engine, Support Agent |
| **Preconditions** | Feature enabled; shopper consents to AI chat terms |
| **Main flow** | 1. Shopper opens "Ask Yvette" and describes a need ("outfit for a summer wedding, size 10, under $150"). 2. Assistant asks clarifying questions if needed. 3. Assistant searches the live catalogue (in-stock only, in the shopper's size) and proposes 2–4 items/outfits with images, prices and reasons. 4. Shopper asks follow-ups or adds items to cart directly from chat. |
| **Alternative flows** | 2a. Question is about an order/return/complaint → assistant looks up order status (logged-in only) or offers hand-off to a human agent. 3a. Nothing matches → assistant says so honestly and suggests closest alternatives or back-in-stock alert. *a. Abusive/off-topic → polite refusal. |
| **Business rules** | Never invent products, prices, stock or policies; clearly labelled as AI; conversation stored for quality review per privacy policy. |
