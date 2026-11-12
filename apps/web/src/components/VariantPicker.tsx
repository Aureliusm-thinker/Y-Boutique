"use client";

import { useState } from "react";
import { formatPrice } from "@/lib/format";
import type { Variant } from "@/lib/types";

export function VariantPicker({ variants }: { variants: Variant[] }) {
  const [selectedId, setSelectedId] = useState<number | null>(
    variants.find((v) => v.inStock)?.id ?? null,
  );
  const selected = variants.find((v) => v.id === selectedId) ?? null;
  const shown = selected ?? variants[0];

  return (
    <div>
      {shown && (
        <p className="text-lg">
          {formatPrice(shown.price, shown.currency)}
          {shown.compareAtPrice != null && shown.compareAtPrice > shown.price && (
            <span className="ml-2 text-stone-400 line-through">
              {formatPrice(shown.compareAtPrice, shown.currency)}
            </span>
          )}
        </p>
      )}

      <fieldset className="mt-6">
        <legend className="text-sm text-stone-700">Size</legend>
        <div className="mt-2 flex flex-wrap gap-2">
          {variants.map((v) => (
            <button
              key={v.id}
              type="button"
              disabled={!v.inStock}
              aria-pressed={v.id === selectedId}
              onClick={() => setSelectedId(v.id)}
              className="min-w-12 border px-3 py-2 text-sm aria-pressed:border-stone-900 aria-pressed:bg-stone-900 aria-pressed:text-white disabled:cursor-not-allowed disabled:text-stone-300 disabled:line-through"
            >
              {v.size ?? v.sku}
            </button>
          ))}
        </div>
      </fieldset>

      {/* Cart & checkout arrive in Sprint 3 (docs/06-mvp-backlog.md, Epic E3). */}
      <button
        type="button"
        disabled
        title="Cart coming in Sprint 3"
        className="mt-6 w-full bg-stone-900 py-3 text-sm uppercase tracking-widest text-white disabled:opacity-50"
      >
        {selected ? "Add to bag" : "Sold out"}
      </button>
    </div>
  );
}
