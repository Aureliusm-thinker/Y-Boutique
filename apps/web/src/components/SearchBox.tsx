"use client";

import Form from "next/form";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useId, useState } from "react";
import { formatPrice } from "@/lib/format";
import type { Suggestions } from "@/lib/types";

const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const EMPTY: Suggestions = { products: [], categories: [] };

/** Header search with autocomplete (FR-SF-04). Submitting goes to /shop?q=… */
export function SearchBox() {
  const router = useRouter();
  const listId = useId();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [results, setResults] = useState<{ q: string; data: Suggestions }>({ q: "", data: EMPTY });

  const trimmed = query.trim();
  useEffect(() => {
    if (trimmed.length < 2) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      fetch(`${PUBLIC_API_URL}/api/v1/catalog/suggest?q=${encodeURIComponent(trimmed)}`, { signal: controller.signal })
        .then((r) => (r.ok ? r.json() : EMPTY))
        .then((data: Suggestions) => setResults({ q: trimmed, data }))
        .catch(() => {});
    }, 150);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [trimmed]);

  const data = trimmed.length >= 2 && results.q === trimmed ? results.data : EMPTY;
  const options = [
    ...data.categories.map((c) => ({ key: `c${c.id}`, href: `/shop?category=${c.slug}`, label: c.name, kind: "Category" })),
    ...data.products.map((p) => ({
      key: `p${p.id}`,
      href: `/products/${p.slug}`,
      label: p.title,
      kind: formatPrice(p.priceFrom, p.currency),
    })),
  ];
  const showList = open && options.length > 0;

  return (
    <Form
      action="/shop"
      role="search"
      className="relative"
      onSubmit={(e) => {
        if (active >= 0 && options[active]) {
          e.preventDefault();
          router.push(options[active].href);
        }
        setOpen(false);
      }}
    >
      <input
        type="search"
        name="q"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
          setActive(-1);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setActive((i) => Math.min(i + 1, options.length - 1));
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setActive((i) => Math.max(i - 1, -1));
          } else if (e.key === "Escape") {
            setOpen(false);
          }
        }}
        placeholder="Search"
        aria-label="Search products"
        role="combobox"
        aria-expanded={showList}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={active >= 0 ? `${listId}-${active}` : undefined}
        autoComplete="off"
        className="w-36 border-b border-stone-300 bg-transparent px-1 py-1 text-sm focus:w-56 focus:border-stone-900 focus:outline-none sm:w-44"
      />
      {showList && (
        <ul
          id={listId}
          role="listbox"
          className="absolute right-0 top-full z-40 mt-2 w-72 border border-stone-200 bg-white py-1 text-sm shadow-lg"
        >
          {options.map((o, i) => (
            <li key={o.key} id={`${listId}-${i}`} role="option" aria-selected={i === active}>
              <Link
                href={o.href}
                onClick={() => setOpen(false)}
                className={`flex justify-between gap-3 px-3 py-2 hover:bg-stone-100 ${i === active ? "bg-stone-100" : ""}`}
              >
                <span>{o.label}</span>
                <span className="shrink-0 text-xs text-stone-500">{o.kind}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Form>
  );
}
