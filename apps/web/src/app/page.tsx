import Link from "next/link";
import { ProductGrid } from "@/components/ProductCard";
import { getProducts } from "@/lib/api";

async function NewArrivals() {
  const { items } = await getProducts();
  return <ProductGrid products={items.slice(0, 8)} />;
}

export default function HomePage() {
  return (
    <>
      {/* Hero — will become admin-editable in Sprint 5 (content module) */}
      <section className="bg-stone-200/60">
        <div className="mx-auto max-w-6xl px-4 py-24 text-center sm:py-32">
          <p className="text-xs uppercase tracking-[0.3em] text-stone-600">New season</p>
          <h1 className="mt-4 font-serif text-5xl leading-tight sm:text-7xl">Curated for you.</h1>
          <p className="mx-auto mt-4 max-w-md text-stone-600">
            Considered pieces in natural fabrics, chosen to be worn and loved for years.
          </p>
          <Link
            href="/shop"
            className="mt-8 inline-block bg-stone-900 px-8 py-3 text-sm uppercase tracking-widest text-white hover:bg-stone-700"
          >
            Shop the collection
          </Link>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="mb-8 font-serif text-3xl">New arrivals</h2>
        <NewArrivals />
      </section>
    </>
  );
}
