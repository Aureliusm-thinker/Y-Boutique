"use client";

import Form from "next/form";
import { useRef } from "react";
import { formatPrice } from "@/lib/format";
import type { Facets, ShopFilters } from "@/lib/types";

/**
 * Filters + sort for /shop. A plain GET form, so the URL always reflects the filters (shareable,
 * crawlable) and it works without JavaScript; with JavaScript it applies as soon as something changes.
 */
export function FilterPanel({ facets, filters }: { facets: Facets; filters: ShopFilters }) {
  const form = useRef<HTMLFormElement>(null);
  const submit = () => form.current?.requestSubmit();
  // Remount inputs when the URL changes so they show the applied values.
  const key = JSON.stringify(filters);

  return (
    <Form ref={form} action="/shop" key={key} className="space-y-6 text-sm" aria-label="Filter products">
      {filters.category && <input type="hidden" name="category" value={filters.category} />}
      {filters.q && <input type="hidden" name="q" value={filters.q} />}

      <label className="block">
        <span className="text-xs uppercase tracking-widest text-stone-500">Sort by</span>
        <select
          name="sort"
          defaultValue={filters.sort ?? (filters.q ? "relevance" : "newest")}
          onChange={submit}
          className="mt-2 w-full border border-stone-300 bg-white px-2 py-2"
        >
          {filters.q && <option value="relevance">Best match</option>}
          <option value="newest">Newest</option>
          <option value="price_asc">Price: low to high</option>
          <option value="price_desc">Price: high to low</option>
        </select>
      </label>

      {facets.sizes.length > 0 && (
        <fieldset>
          <legend className="text-xs uppercase tracking-widest text-stone-500">Size</legend>
          <div className="mt-2 flex flex-wrap gap-2">
            {facets.sizes.map((s) => (
              <label key={s} className="cursor-pointer">
                <input
                  type="checkbox"
                  name="sizes"
                  value={s}
                  defaultChecked={filters.sizes?.some((x) => x.toUpperCase() === s.toUpperCase())}
                  onChange={submit}
                  className="peer sr-only"
                />
                <span className="inline-block min-w-10 border border-stone-300 px-2 py-1 text-center peer-checked:border-stone-900 peer-checked:bg-stone-900 peer-checked:text-white peer-focus-visible:ring-2 peer-focus-visible:ring-stone-400">
                  {s}
                </span>
              </label>
            ))}
          </div>
        </fieldset>
      )}

      {facets.colours.length > 0 && (
        <fieldset>
          <legend className="text-xs uppercase tracking-widest text-stone-500">Colour</legend>
          <div className="mt-2 space-y-1">
            {facets.colours.map((c) => (
              <label key={c} className="flex items-center gap-2">
                <input
                  type="checkbox"
                  name="colours"
                  value={c.toLowerCase()}
                  defaultChecked={filters.colours?.some((x) => x.toLowerCase() === c.toLowerCase())}
                  onChange={submit}
                />
                {c}
              </label>
            ))}
          </div>
        </fieldset>
      )}

      <fieldset>
        <legend className="text-xs uppercase tracking-widest text-stone-500">Price (KSh)</legend>
        <div className="mt-2 flex items-center gap-2">
          <input
            type="number"
            name="minPrice"
            min={0}
            step={100}
            inputMode="numeric"
            placeholder={facets.minPrice != null ? String(Math.floor(facets.minPrice)) : "Min"}
            defaultValue={filters.minPrice}
            aria-label="Minimum price"
            className="w-full border border-stone-300 px-2 py-1.5"
          />
          <span aria-hidden>–</span>
          <input
            type="number"
            name="maxPrice"
            min={0}
            step={100}
            inputMode="numeric"
            placeholder={facets.maxPrice != null ? String(Math.ceil(facets.maxPrice)) : "Max"}
            defaultValue={filters.maxPrice}
            aria-label="Maximum price"
            className="w-full border border-stone-300 px-2 py-1.5"
          />
        </div>
        {facets.minPrice != null && facets.maxPrice != null && (
          <p className="mt-1 text-xs text-stone-500">
            {formatPrice(facets.minPrice, "KES")} – {formatPrice(facets.maxPrice, "KES")}
          </p>
        )}
      </fieldset>

      <label className="flex items-center gap-2">
        <input type="checkbox" name="inStock" value="true" defaultChecked={filters.inStock} onChange={submit} />
        In stock only
      </label>

      <button className="w-full bg-stone-900 py-2 text-xs uppercase tracking-widest text-white hover:bg-stone-700">
        Apply filters
      </button>
    </Form>
  );
}
