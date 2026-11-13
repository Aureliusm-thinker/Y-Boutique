"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { formatPrice } from "@/lib/format";

// Recently viewed products, kept on this device only (FR-SF-07). No personal data, just catalogue items.
const KEY = "yvette.recentlyViewed";
const MAX = 8;

export type RecentItem = { slug: string; title: string; imageUrl: string | null; price: number | null; currency: string | null };

function read(): RecentItem[] {
  try {
    const raw = window.localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as RecentItem[]) : [];
  } catch {
    return [];
  }
}

/** Records a product view. Render on the product page. */
export function RememberView({ item }: { item: RecentItem }) {
  const key = JSON.stringify(item);
  useEffect(() => {
    const current: RecentItem = JSON.parse(key);
    const list = [current, ...read().filter((i) => i.slug !== current.slug)].slice(0, MAX);
    try {
      window.localStorage.setItem(KEY, JSON.stringify(list));
    } catch {
      /* storage full or blocked: not important */
    }
  }, [key]);
  return null;
}

export function RecentlyViewed({ excludeSlug }: { excludeSlug?: string }) {
  const [items, setItems] = useState<RecentItem[]>([]);
  useEffect(() => {
    // localStorage is only available after mount.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setItems(read().filter((i) => i.slug !== excludeSlug).slice(0, 4));
  }, [excludeSlug]);
  if (items.length === 0) return null;
  return (
    <section className="mt-20">
      <h2 className="mb-6 font-serif text-2xl">Recently viewed</h2>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {items.map((i) => (
          <Link key={i.slug} href={`/products/${i.slug}`} className="group block">
            <div className="relative aspect-[4/5] bg-stone-100">
              {i.imageUrl && <Image src={i.imageUrl} alt="" fill sizes="25vw" className="object-cover" />}
            </div>
            <p className="mt-2 text-sm">{i.title}</p>
            <p className="text-sm text-stone-600">{formatPrice(i.price, i.currency)}</p>
          </Link>
        ))}
      </div>
    </section>
  );
}
