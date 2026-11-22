"use client";

import { useEffect, useState } from "react";
import { ProductGrid, ProductGridSkeleton } from "@/components/ProductCard";
import { consentedVisitorId, track } from "@/lib/analytics";
import type { ForYou } from "@/lib/types";

const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * "Picked for you": products chosen from this visitor's recent searches and browsing (only if they accepted
 * analytics), otherwise popular products. Fetched in the browser because the visitor id lives on the device,
 * which keeps the surrounding page cacheable for everyone.
 */
export function PickedForYou({
  placement,
  fallbackTitle = "Popular right now",
  excludeProductId,
  limit = 4,
  className = "mt-20",
}: {
  /** Where the row is shown, recorded with impressions and clicks (e.g. "home", "empty_cart"). */
  placement: string;
  fallbackTitle?: string;
  excludeProductId?: number;
  limit?: number;
  className?: string;
}) {
  const [data, setData] = useState<ForYou | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const qs = new URLSearchParams({ limit: String(limit) });
    const visitor = consentedVisitorId();
    if (visitor) qs.set("visitor", visitor);
    if (excludeProductId != null) qs.set("exclude", String(excludeProductId));
    fetch(`${PUBLIC_API_URL}/api/v1/catalog/recommendations/for-you?${qs}`, { signal: controller.signal })
      .then((res) => (res.ok ? (res.json() as Promise<ForYou>) : Promise.reject(new Error(String(res.status)))))
      .then((result) => {
        setData(result);
        if (result.items.length > 0) {
          track("recommendation_impression", {
            placement,
            personalised: result.personalised,
            productIds: result.items.map((p) => p.id),
          });
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true); // recommendations must never break the page
      });
    return () => controller.abort();
  }, [placement, excludeProductId, limit]);

  if (failed || (data && data.items.length === 0)) return null;

  const title = data?.personalised ? "Picked for you" : fallbackTitle;
  const reason = !data?.personalised
    ? null
    : data.basedOnSearches.length > 0
      ? `Based on your searches for ${data.basedOnSearches.map((q) => `“${q}”`).join(", ")}`
      : "Based on what you’ve been browsing";

  return (
    <section className={className} aria-busy={!data}>
      <h2 className="font-serif text-2xl">{data ? title : " "}</h2>
      <p className="mb-6 mt-1 min-h-5 text-sm text-stone-500">{reason}</p>
      {data ? (
        <div
          onClickCapture={(e) => {
            const link = (e.target as HTMLElement).closest("a");
            const slug = link?.getAttribute("href")?.split("/products/")[1];
            const product = data.items.find((p) => p.slug === slug);
            if (product) {
              track("recommendation_click", { placement, personalised: data.personalised, productId: product.id });
            }
          }}
        >
          <ProductGrid products={data.items} />
        </div>
      ) : (
        <ProductGridSkeleton />
      )}
    </section>
  );
}
