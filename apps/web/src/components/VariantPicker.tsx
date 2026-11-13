"use client";

import { useState } from "react";
import { formatPrice } from "@/lib/format";
import type { Variant } from "@/lib/types";
import { AddToBag } from "./AddToBag";

const unique = (values: (string | null)[]) => [...new Set(values.filter((v): v is string => Boolean(v)))];

/** Colour + size selection; price and stock follow the chosen variant (FR-SF-05). */
export function VariantPicker({ variants, productTitle }: { variants: Variant[]; productTitle: string }) {
  const colours = unique(variants.map((v) => v.colour));
  const firstInStock = variants.find((v) => v.inStock) ?? variants[0];
  const [colour, setColour] = useState<string | null>(firstInStock?.colour ?? null);
  const [selectedId, setSelectedId] = useState<number | null>(firstInStock?.inStock ? firstInStock.id : null);

  const forColour = colours.length > 1 ? variants.filter((v) => v.colour === colour) : variants;
  const selected = variants.find((v) => v.id === selectedId) ?? null;
  const shown = selected ?? forColour[0] ?? variants[0];
  const allSoldOut = variants.every((v) => !v.inStock);

  return (
    <div>
      {shown && (
        <p className="text-lg" aria-live="polite">
          {formatPrice(shown.price, shown.currency)}
          {shown.compareAtPrice != null && shown.compareAtPrice > shown.price && (
            <span className="ml-2 text-stone-400 line-through">{formatPrice(shown.compareAtPrice, shown.currency)}</span>
          )}
        </p>
      )}

      {colours.length > 1 && (
        <fieldset className="mt-6">
          <legend className="text-sm text-stone-700">
            Colour: <span className="text-stone-900">{colour}</span>
          </legend>
          <div className="mt-2 flex flex-wrap gap-2">
            {colours.map((c) => (
              <button
                key={c}
                type="button"
                aria-pressed={c === colour}
                onClick={() => {
                  setColour(c);
                  const match = variants.find((v) => v.colour === c && v.size === selected?.size && v.inStock);
                  setSelectedId(match?.id ?? null);
                }}
                className="border px-3 py-1.5 text-sm aria-pressed:border-stone-900 aria-pressed:bg-stone-900 aria-pressed:text-white"
              >
                {c}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      <fieldset className="mt-6">
        <legend className="text-sm text-stone-700">Size</legend>
        <div className="mt-2 flex flex-wrap gap-2">
          {forColour.map((v) => (
            <button
              key={v.id}
              type="button"
              disabled={!v.inStock}
              aria-pressed={v.id === selectedId}
              aria-label={`${v.size ?? v.sku}${v.inStock ? "" : " (sold out)"}`}
              onClick={() => setSelectedId(v.id)}
              className="min-w-12 border px-3 py-2 text-sm aria-pressed:border-stone-900 aria-pressed:bg-stone-900 aria-pressed:text-white disabled:cursor-not-allowed disabled:text-stone-300 disabled:line-through"
            >
              {v.size ?? v.sku}
            </button>
          ))}
        </div>
      </fieldset>

      <p className="mt-3 h-5 text-sm text-amber-700" aria-live="polite">
        {selected?.stockLeft ? `Only ${selected.stockLeft} left` : ""}
      </p>

      <AddToBag
        variantId={selected?.id ?? null}
        label={allSoldOut ? "Sold out" : selected ? "Add to bag" : "Choose a size"}
        productTitle={productTitle}
      />
    </div>
  );
}
