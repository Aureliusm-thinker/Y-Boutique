# Yvette storefront (Next.js)

```bash
cp .env.example .env.local   # points at the Java API on http://localhost:8080
npm install
npm run dev                  # http://localhost:3000
```

- `src/app` — routes: `/` home, `/shop` (category filter), `/products/[slug]`
- `src/lib/api.ts` — server-side, cached (`"use cache"`) calls to the Java API
- `src/lib/analytics.ts` — consent-gated first-party event tracking (AI data foundation)
- `src/components` — UI components

This project uses Next.js 16 with Cache Components. See `AGENTS.md` and `node_modules/next/dist/docs/` before changing data-fetching code.
