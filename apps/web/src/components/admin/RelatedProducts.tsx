"use client";

import { useState, useTransition } from "react";
import { findProducts, setRelatedProducts } from "@/app/admin/(panel)/products/actions";
import type { ActionResult, ProductStatus } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

type Item = { id: number; title: string; status: ProductStatus | string };

/** Curated "complete the look" pairs shown on the product page (FR-SF-06). */
export function RelatedProducts({
  productId,
  initial,
  canEdit,
}: {
  productId: number;
  initial: Item[];
  canEdit: boolean;
}) {
  const [items, setItems] = useState<Item[]>(initial);
  const [query, setQuery] = useState("");
  const [matches, setMatches] = useState<Item[]>([]);
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, startTransition] = useTransition();

  const save = (next: Item[]) => {
    setItems(next);
    startTransition(async () => setResult(await setRelatedProducts(productId, next.map((i) => i.id))));
  };

  return (
    <section className="admin-card">
      <h2 className="font-medium">Complete the look</h2>
      <p className="mb-3 mt-1 text-xs text-stone-500">Shown under the product. Only live products appear in the shop.</p>
      {items.length === 0 && <p className="text-sm text-stone-500">None yet.</p>}
      <ul className="space-y-1 text-sm">
        {items.map((i) => (
          <li key={i.id} className="flex items-center justify-between gap-2">
            <span>
              {i.title} {i.status !== "ACTIVE" && <span className="text-xs text-stone-500">({String(i.status).toLowerCase()})</span>}
            </span>
            {canEdit && (
              <button
                type="button"
                className="text-xs text-red-700 hover:underline"
                disabled={pending}
                onClick={() => save(items.filter((x) => x.id !== i.id))}
              >
                Remove
              </button>
            )}
          </li>
        ))}
      </ul>
      {canEdit && items.length < 8 && (
        <div className="mt-3">
          <input
            value={query}
            onChange={(e) => {
              const q = e.target.value;
              setQuery(q);
              if (q.trim().length >= 2) {
                startTransition(async () => setMatches(await findProducts(q.trim())));
              } else {
                setMatches([]);
              }
            }}
            placeholder="Find a product to add…"
            aria-label="Find a product to add"
            className="admin-input"
          />
          {matches.length > 0 && (
            <ul className="mt-1 border border-stone-200 text-sm">
              {matches
                .filter((m) => m.id !== productId && !items.some((i) => i.id === m.id))
                .map((m) => (
                  <li key={m.id}>
                    <button
                      type="button"
                      className="w-full px-2 py-1.5 text-left hover:bg-stone-100"
                      onClick={() => {
                        save([...items, m]);
                        setQuery("");
                        setMatches([]);
                      }}
                    >
                      + {m.title}
                    </button>
                  </li>
                ))}
            </ul>
          )}
        </div>
      )}
      <div className="mt-2">
        <ActionMessage result={result && !result.ok ? result : null} />
      </div>
    </section>
  );
}
