import Link from "next/link";
import { Hero } from "@/components/Hero";
import { ProductGrid } from "@/components/ProductCard";
import { RecentlyViewed } from "@/components/RecentlyViewed";
import { getCategories, getProducts, getSiteContent, type HomeContent } from "@/lib/api";

async function Featured({ home }: { home: HomeContent }) {
  const { items } = await getProducts(home.featuredCategory ? { category: home.featuredCategory } : {});
  return <ProductGrid products={items.slice(0, 8)} />;
}

async function Categories() {
  const categories = await getCategories();
  if (categories.length === 0) return null;
  return (
    <section className="mx-auto max-w-6xl px-4 pb-4">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {categories.slice(0, 6).map((c) => (
          <Link
            key={c.id}
            href={`/shop?category=${c.slug}`}
            className="border border-stone-300 py-6 text-center font-serif text-xl hover:border-stone-900"
          >
            {c.name}
          </Link>
        ))}
      </div>
    </section>
  );
}

export default async function HomePage() {
  const { home } = await getSiteContent();
  return (
    <>
      <Hero home={home} />
      <section className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="mb-8 font-serif text-3xl">{home.featuredTitle ?? "New arrivals"}</h2>
        <Featured home={home} />
        <RecentlyViewed />
      </section>
      <Categories />
    </>
  );
}
