import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { FilterPanel } from "@/components/FilterPanel";
import { ProductGrid, ProductGridSkeleton } from "@/components/ProductCard";
import { TrackView } from "@/components/TrackView";
import { getCategories, getFacets, getProducts } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import { parseShopFilters, shopHref } from "@/lib/shop-filters";
import type { ShopFilters } from "@/lib/types";

export async function generateMetadata({ searchParams }: PageProps<"/shop">): Promise<Metadata> {
  const f = parseShopFilters(await searchParams);
  const categories = await getCategories();
  const category = categories.find((c) => c.slug === f.category);
  const title = f.q ? `Search: ${f.q}` : (category?.name ?? "Shop all");
  const filtered = Boolean(f.q || f.sizes?.length || f.colours?.length || f.minPrice || f.maxPrice || f.inStock || f.page);
  return {
    title,
    description: category ? `Shop ${category.name.toLowerCase()} at Yvette.` : "Shop the full Yvette collection.",
    // Filtered/search variants shouldn't compete with the category page in search engines.
    alternates: { canonical: category ? `/shop?category=${category.slug}` : "/shop" },
    robots: filtered ? { index: false, follow: true } : undefined,
  };
}

async function CategoryNav({ filters }: { filters: ShopFilters }) {
  const categories = await getCategories();
  const linkClass = (isActive: boolean) =>
    `rounded-full border px-4 py-1.5 text-sm ${isActive ? "border-stone-900 bg-stone-900 text-white" : "border-stone-300 hover:border-stone-900"}`;
  return (
    <nav aria-label="Categories" className="mb-8 flex flex-wrap gap-2">
      <Link href={shopHref({ ...filters, category: undefined, page: undefined })} className={linkClass(!filters.category)}>
        All
      </Link>
      {categories.map((c) => (
        <Link
          key={c.id}
          href={shopHref({ q: filters.q, category: c.slug })}
          className={linkClass(filters.category === c.slug)}
        >
          {c.name}
        </Link>
      ))}
    </nav>
  );
}

/** Removable chips for each applied filter. */
function ActiveFilters({ filters }: { filters: ShopFilters }) {
  const chips: { label: string; href: string }[] = [];
  const base = { ...filters, page: undefined };
  for (const s of filters.sizes ?? []) {
    chips.push({ label: `Size ${s}`, href: shopHref({ ...base, sizes: filters.sizes?.filter((x) => x !== s) }) });
  }
  for (const c of filters.colours ?? []) {
    chips.push({ label: c, href: shopHref({ ...base, colours: filters.colours?.filter((x) => x !== c) }) });
  }
  if (filters.minPrice != null) chips.push({ label: `From ${formatPrice(filters.minPrice, "KES")}`, href: shopHref({ ...base, minPrice: undefined }) });
  if (filters.maxPrice != null) chips.push({ label: `Up to ${formatPrice(filters.maxPrice, "KES")}`, href: shopHref({ ...base, maxPrice: undefined }) });
  if (filters.inStock) chips.push({ label: "In stock", href: shopHref({ ...base, inStock: false }) });
  if (chips.length === 0) return null;
  return (
    <div className="mb-4 flex flex-wrap items-center gap-2 text-xs">
      {chips.map((c) => (
        <Link key={c.label} href={c.href} className="rounded-full bg-stone-100 px-3 py-1 capitalize hover:bg-stone-200">
          {c.label} <span aria-label="remove">×</span>
        </Link>
      ))}
      <Link href={shopHref({ category: filters.category, q: filters.q })} className="underline">
        Clear all
      </Link>
    </div>
  );
}

async function Filters({ filters }: { filters: ShopFilters }) {
  const facets = await getFacets(filters.category);
  return <FilterPanel facets={facets} filters={filters} />;
}

async function NoResults({ filters }: { filters: ShopFilters }) {
  const [categories, newest] = await Promise.all([getCategories(), getProducts({})]);
  return (
    <div className="py-8">
      <p className="text-lg">
        {filters.q ? <>We couldn&apos;t find anything for “{filters.q}”.</> : "Nothing matches these filters."}
      </p>
      <p className="mt-2 text-sm text-stone-600">
        Try fewer filters, check the spelling, or browse a category:
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        {categories.map((c) => (
          <Link key={c.id} href={`/shop?category=${c.slug}`} className="rounded-full border border-stone-300 px-4 py-1.5 text-sm hover:border-stone-900">
            {c.name}
          </Link>
        ))}
      </div>
      {newest.items.length > 0 && (
        <>
          <h2 className="mb-6 mt-12 font-serif text-2xl">New arrivals you might like</h2>
          <ProductGrid products={newest.items.slice(0, 4)} />
        </>
      )}
    </div>
  );
}

async function Results({ filters }: { filters: ShopFilters }) {
  const result = await getProducts(filters);
  const page = filters.page ?? 0;
  return (
    <>
      {filters.q && (
        <TrackView name="search" properties={{ query: filters.q, results: result.totalItems }} />
      )}
      <p className="mb-4 text-sm text-stone-500" aria-live="polite">
        {result.totalItems} {result.totalItems === 1 ? "item" : "items"}
      </p>
      <ActiveFilters filters={filters} />
      {result.totalItems === 0 ? (
        <NoResults filters={filters} />
      ) : (
        <ProductGrid products={result.items} columns={3} />
      )}
      {result.totalPages > 1 && (
        <nav aria-label="Pages" className="mt-12 flex items-center justify-center gap-4 text-sm">
          {page > 0 && <Link href={shopHref({ ...filters, page: page - 1 || undefined })} className="underline">Previous</Link>}
          <span>
            Page {page + 1} of {result.totalPages}
          </span>
          {page + 1 < result.totalPages && <Link href={shopHref({ ...filters, page: page + 1 })} className="underline">Next</Link>}
        </nav>
      )}
    </>
  );
}

async function ShopContent({ searchParams }: Pick<PageProps<"/shop">, "searchParams">) {
  const filters = parseShopFilters(await searchParams);
  const categories = await getCategories();
  const category = categories.find((c) => c.slug === filters.category);
  return (
    <>
      <h1 className="mb-6 font-serif text-4xl">
        {filters.q ? <>Results for “{filters.q}”</> : (category?.name ?? "Shop all")}
      </h1>
      <CategoryNav filters={filters} />
      <div className="grid gap-10 md:grid-cols-[14rem_1fr]">
        <aside>
          <details className="md:hidden">
            <summary className="cursor-pointer border border-stone-300 px-4 py-2 text-sm">Filter &amp; sort</summary>
            <div className="mt-4">
              <Filters filters={filters} />
            </div>
          </details>
          <div className="hidden md:block">
            <Filters filters={filters} />
          </div>
        </aside>
        <div>
          <Results filters={filters} />
        </div>
      </div>
    </>
  );
}

export default function ShopPage({ searchParams }: PageProps<"/shop">) {
  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <Suspense
        fallback={
          <>
            <div className="mb-6 h-10 w-48 animate-pulse bg-stone-100" />
            <ProductGridSkeleton />
          </>
        }
      >
        <ShopContent searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
