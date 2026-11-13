import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AccountShell } from "@/components/account/AccountShell";
import { ProductGrid } from "@/components/ProductCard";
import { requireCustomer } from "@/lib/account";
import { shopFetch } from "@/lib/shop-api";
import type { ProductSummary } from "@/lib/types";

export const metadata: Metadata = { title: "Wishlist", robots: { index: false } };

async function Content() {
  await requireCustomer("/account/wishlist");
  const items = await shopFetch<ProductSummary[]>("/api/v1/account/wishlist");
  if (items.length === 0) {
    return (
      <p className="text-stone-600">
        Nothing saved yet. Tap the heart on any product to save it here. <Link href="/shop" className="underline">Browse the shop</Link>
      </p>
    );
  }
  return <ProductGrid products={items} columns={3} />;
}

export default function WishlistPage() {
  return (
    <AccountShell title="Wishlist" active="/account/wishlist">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content />
      </Suspense>
    </AccountShell>
  );
}
