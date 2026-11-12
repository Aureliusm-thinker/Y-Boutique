# AI Service (Phase 3 — placeholder)

Python (FastAPI) service for the AI/ML features described in [docs/04-ai-ml-strategy.md](../../docs/04-ai-ml-strategy.md):

| Endpoint (planned) | Feature |
|---|---|
| `/recommendations` | Personalised & similar products |
| `/search/semantic` | Natural-language search over product embeddings (pgvector) |
| `/stylist/chat` | "Ask Yvette" AI stylist (LLM with tools that call the Java API) |
| `/size-advice` | Size & fit recommendation |
| `/content/generate` | Product descriptions / SEO / tags for admin (human-approved) |
| `/forecast` | Nightly demand forecasting & restock suggestions |

The Java API calls this service through `com.yvette.api.ai` and falls back to rule-based results if it is unavailable, so AI problems never block shopping or checkout.

Until Phase 3, the job here is done by the data foundation already in place: the `events` table and structured product attributes in the Java API.
