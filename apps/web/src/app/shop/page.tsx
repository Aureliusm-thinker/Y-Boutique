import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { ProductGrid, ProductGridSkeleton } from "@/components/ProductCard";
import { getCategories, getProducts } from "@/lib/api";

export const metadata: Metadata = { title: "Shop" };

async function CategoryFilter({ searchParams }: Pick<PageProps<"/shop">, "searchParams">) {
  const [{ category }, categories] = await Promise.all([searchParams, getCategories()]);
  const active = typeof category === "string" ? category : undefined;
  const linkClass = (isActive: boolean) =>
    `rounded-full border px-4 py-1.5 text-sm ${isActive ? "border-stone-900 bg-stone-900 text-white" : "border-stone-300 hover:border-stone-900"}`;
  return (
    <nav aria-label="Categories" className="mb-8 flex flex-wrap gap-2">
      <Link href="/shop" className={linkClass(!active)}>
        All
      </Link>
      {categories.map((c) => (
        <Link key={c.id} href={`/shop?category=${c.slug}`} className={linkClass(active === c.slug)}>
          {c.name}
        </Link>
      ))}
    </nav>
  );
}

async function Results({ searchParams }: Pick<PageProps<"/shop">, "searchParams">) {
  const { category } = await searchParams;
  const { items } = await getProducts(typeof category === "string" ? category : undefined);
  return <ProductGrid products={items} />;
}

export default function ShopPage({ searchParams }: PageProps<"/shop">) {
  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="mb-6 font-serif text-4xl">Shop</h1>
      {/* Filters for size, colour and price follow in Sprint 2 (FR-SF-03). */}
      <Suspense fallback={<div className="mb-8 h-9" />}>
        <CategoryFilter searchParams={searchParams} />
      </Suspense>
      <Suspense fallback={<ProductGridSkeleton />}>
        <Results searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
