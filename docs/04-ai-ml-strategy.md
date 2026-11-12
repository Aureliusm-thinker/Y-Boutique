# 04 — AI / ML Strategy

| Field | Value |
|---|---|
| Version | 0.1 (draft) |
| Date | 2026-11-11 |

---

## 1. Principles

1. **AI comes after the basics work.** The MVP must sell well without AI. AI is introduced in Phase 3, behind feature flags.
2. **Collect the data now, build the models later.** ML needs history. The MVP must capture clean events and rich product attributes from day one (see §3).
3. **Start simple, earn complexity.** Rules → off-the-shelf models / LLM APIs → custom models only when data and ROI justify it.
4. **Measure everything.** Every AI feature ships with an A/B test and a success metric. If it doesn't move the metric, we remove or rework it.
5. **Responsible by design.** Grounded outputs, privacy and consent, human oversight, no inference of sensitive attributes, clear "AI" labelling.

## 2. AI Feature Portfolio

| # | Feature | Actor(s) served | Business value | Approach (start → mature) | Data needed | Phase |
|---|---|---|---|---|---|---|
| AI-1 | **Personalised recommendations** ("For you", "You may also like", "Frequently bought together", cart & email recs) | Customer, Guest | ↑ AOV, ↑ conversion, ↑ retention | Rule-based (same category/tags, best sellers) → item-to-item co-occurrence → embeddings + collaborative filtering / two-tower model | Views, carts, purchases, product attributes | 2 (rules) → 3 (ML) |
| AI-2 | **Semantic search** ("flowy white dress for a beach wedding") | Customer, Guest | ↑ search conversion, fewer zero-result searches | Product text + image embeddings in a vector index, hybrid with keyword search, LLM query understanding | Product titles, descriptions, attributes, images; search logs | 3a |
| AI-3 | **AI Stylist assistant – "Ask Yvette"** | Customer, Guest, Support Agent (hand-off) | Personal-shopper experience at scale; fewer support tickets | LLM with tool use (catalogue search, stock check, size advice, add-to-cart, order lookup, human hand-off); retrieval over policies/FAQ | Catalogue, policies, FAQ, order data (authenticated) | 3b |
| AI-4 | **Size & fit recommendation** | Customer, Guest | ↓ return rate, ↑ purchase confidence | Size charts + review fit feedback rules → model using customer's kept vs. returned sizes per brand/category | Size charts, purchases, returns with reasons, fit feedback in reviews | 3c |
| AI-5 | **AI content generation for admin** (descriptions, SEO titles/meta, attribute tagging from images, alt text) | Inventory Staff, Marketing | Faster listing, better SEO & accessibility, consistent tagging | LLM / vision model; staff always reviews & approves | Product images, basic specs, brand voice guide | 3c |
| AI-6 | **Demand forecasting & restock suggestions** | Owner, Inventory Staff | ↓ stock-outs and dead stock | Moving averages + seasonality → gradient-boosted / time-series model | Sales history per SKU, stock levels, promotions calendar | 3d |
| AI-7 | **Review summarisation** ("Customers say: runs small, soft fabric") | Customer | Faster decisions | LLM summarisation, refreshed nightly | Approved reviews | 3 |
| AI-8 | **Customer segmentation & churn/next-purchase prediction** | Marketing | Better targeted campaigns | RFM segmentation → ML propensity models | Orders, engagement | 4 |
| AI-9 | **Visual search / "Shop the look" from a photo** | Customer | Social-media-driven discovery | Image embeddings (CLIP-style) similarity | Product images | 4 |
| AI-10 | **Fraud risk scoring** | Owner, Payment Gateway | ↓ chargebacks | Use payment provider's built-in fraud tools first | Payment signals | 1 (provider) |

## 3. Data Foundation — What the MVP Must Capture

### 3.1 Behavioural events (first-party, consent-aware)
| Event | Key properties |
|---|---|
| `page_view` | url, referrer, session_id, user_id (if any), device |
| `product_view` | product_id, variant_id, source (search/collection/rec/chat), position |
| `search` | query, filters, results_count, clicked_product_ids |
| `add_to_cart` / `remove_from_cart` | product_id, variant_id, qty, price, source |
| `wishlist_add` / `wishlist_remove` | product_id |
| `begin_checkout` | cart_value, item_count |
| `purchase` | order_id, items, value, discount, shipping |
| `return_requested` | order_item_id, reason, size_issue (small/large) |
| `review_submitted` | rating, fit_feedback |
| `recommendation_impression` / `recommendation_click` | model_version, slot, product_ids (Phase 2+) |

Every event carries: `event_id`, `timestamp`, `anonymous_id`, `user_id` (nullable), `session_id`, `consent_state`.

### 3.2 Product attributes (structured, captured in admin)
Category, sub-category, colour (normalised palette), pattern, material/fabric, fit (slim/regular/relaxed), length, neckline/sleeve (where relevant), occasion (work/casual/evening/wedding/holiday), season, style tags (minimal, boho, classic, street…), price band, size chart reference.

### 3.3 Customer signals
Style profile (opt-in), sizes purchased & kept vs. returned, communication preferences, consent records.

## 4. Reference Architecture (AI layer)

```
 Storefront / Admin (Next.js)
        │  REST/GraphQL
        ▼
 Core Commerce API ──── PostgreSQL (orders, products, customers, events)
        │                    │
        │                    ├── pgvector (product & query embeddings)
        │                    └── nightly export → Analytics store / feature tables
        ▼
 AI Service (Python, FastAPI)
   ├── /recommendations   (rules → ML models)
   ├── /search/semantic   (embeddings + hybrid ranking)
   ├── /stylist/chat      (LLM orchestration with tools → calls Core API)
   ├── /size-advice
   ├── /content/generate  (admin only)
   └── /forecast          (batch, nightly)
        │
        ├── LLM provider API (e.g., Claude) — chat, content generation, query understanding
        ├── Embedding model (text + image)
        └── Model registry / experiment tracking (e.g., MLflow) — Phase 3+
```

Key design choices:
- **AI service is separate** from the core commerce API, so AI failures never break checkout (graceful fallback to rules/best sellers).
- **Assistant uses tools, not memory**, for facts: prices, stock and policies always come from live APIs.
- **Feature flags + experiment framework** wrap every AI surface.

## 5. Rollout Plan

| Step | When | Deliverable | Exit criteria |
|---|---|---|---|
| 0 | MVP (Phase 1) | Event tracking, product attributes, consent, data export | Events validated end-to-end; attributes ≥ 95% complete |
| 1 | Phase 2 | Rule-based recs; search logs analysed; brand voice & policy docs written | Baseline metrics recorded |
| 2 | Phase 3a | Semantic/hybrid search + ML recommendations (A/B) | ↑ search CTR, ↑ rec CTR vs. rules |
| 3 | Phase 3b | AI Stylist beta (logged-in users, then all) | Eval suite pass rate ≥ 95%; hand-off works; cost per chat within budget |
| 4 | Phase 3c | Size & fit, admin content generation, review summaries | ↓ size-related returns; listing time ↓ 50% |
| 5 | Phase 3d | Forecasting & restock suggestions | Forecast error (MAPE) better than naive baseline |

## 6. Evaluation & Monitoring
- **Offline:** recommendation precision@k / recall@k, search NDCG, forecast MAPE, assistant eval set (100+ scripted shopper questions with expected behaviour: correct products, no hallucinated items, correct refusals/hand-offs).
- **Online:** A/B tests on CTR, conversion, AOV, return rate; assistant CSAT thumbs up/down; hand-off rate.
- **Operational:** latency, error rate, LLM token cost per session/day, fallback rate.

## 7. Responsible AI Guardrails
- AI assistant clearly labelled; users can reach a human.
- Only recommend in-stock products; never state prices/policies not returned by our APIs.
- No use of sensitive attributes (race, religion, health, body shape inferred from photos, etc.) for personalisation.
- Personalisation respects consent; users can opt out and see/delete their data.
- Prompt-injection defences: treat user input and product/review text as untrusted; tools enforce authorisation server-side (e.g., order lookup only for the logged-in owner of the order).
- Admin-generated content always reviewed by a human before publishing.
- Logs of AI conversations retained per privacy policy, with PII minimised.

## 8. AI Cost Drivers
- LLM tokens (assistant chat, content generation, summaries) — control via caching, smaller models for simple tasks, rate limits.
- Embedding generation (one-off per product + on change).
- Model training/hosting (minimal at first — batch jobs on a schedule).
