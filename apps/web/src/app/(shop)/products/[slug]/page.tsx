import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ProductGallery } from "@/components/ProductGallery";
import { ProductGrid } from "@/components/ProductCard";
import { RecentlyViewed, RememberView } from "@/components/RecentlyViewed";
import { SizeGuide } from "@/components/SizeGuide";
import { TrackView } from "@/components/TrackView";
import { VariantPicker } from "@/components/VariantPicker";
import { WishlistButton } from "@/components/WishlistButton";
import { getProduct } from "@/lib/api";
import type { ProductDetail } from "@/lib/types";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export async function generateMetadata({ params }: PageProps<"/products/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const product = await getProduct(slug);
  if (!product) return { title: "Not found" };
  const description = product.seoDescription ?? product.description?.slice(0, 160) ?? undefined;
  return {
    title: product.seoTitle ?? product.title,
    description,
    alternates: { canonical: `/products/${product.slug}` },
    openGraph: {
      title: product.title,
      description,
      type: "website",
      images: product.images.slice(0, 1).map((i) => ({ url: i.url, alt: i.altText ?? product.title })),
    },
  };
}

/** schema.org Product data so search engines can show price and availability. */
function ProductJsonLd({ product }: { product: ProductDetail }) {
  const data = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.title,
    description: product.description ?? undefined,
    image: product.images.map((i) => i.url),
    sku: product.variants[0]?.sku,
    material: product.material ?? undefined,
    category: product.category?.name,
    brand: { "@type": "Brand", name: "Yvette" },
    offers: product.variants.map((v) => ({
      "@type": "Offer",
      sku: v.sku,
      price: v.price,
      priceCurrency: v.currency,
      availability: v.inStock ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
      url: `${SITE_URL}/products/${product.slug}`,
    })),
  };
  return (
    <script
      type="application/ld+json"
      // JSON.stringify output with "<" escaped so it can't close the script tag.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }}
    />
  );
}

async function ProductContent({ params }: Pick<PageProps<"/products/[slug]">, "params">) {
  const { slug } = await params;
  const product = await getProduct(slug);
  if (!product) notFound();

  const details = [
    ["Material", product.material],
    ["Fit", product.fit],
    ["Occasion", product.occasion],
    ["Season", product.season],
  ].filter(([, value]) => value);
  const cheapest = product.variants.reduce<number | null>((m, v) => (m == null || v.price < m ? v.price : m), null);

  return (
    <>
      <ProductJsonLd product={product} />
      <TrackView name="product_view" properties={{ productId: product.id, slug: product.slug }} />
      <RememberView
        item={{
          slug: product.slug,
          title: product.title,
          imageUrl: product.images[0]?.url ?? null,
          price: cheapest,
          currency: product.variants[0]?.currency ?? "KES",
        }}
      />

      <nav aria-label="Breadcrumb" className="mb-6 text-xs text-stone-500">
        <Link href="/shop" className="hover:underline">
          Shop
        </Link>
        {product.category && (
          <>
            {" / "}
            <Link href={`/shop?category=${product.category.slug}`} className="hover:underline">
              {product.category.name}
            </Link>
          </>
        )}
      </nav>

      <div className="grid gap-10 md:grid-cols-2">
        <ProductGallery images={product.images} title={product.title} />

        <div className="md:sticky md:top-8 md:self-start">
          {product.category && (
            <p className="text-xs uppercase tracking-[0.2em] text-stone-500">{product.category.name}</p>
          )}
          <h1 className="mt-2 font-serif text-4xl">{product.title}</h1>
          <div className="mt-4">
            <VariantPicker variants={product.variants} productTitle={product.title} />
          </div>
          <div className="mt-4 flex items-center justify-between">
            <SizeGuide />
            <WishlistButton productId={product.id} slug={product.slug} />
          </div>
          {product.description && <p className="mt-8 leading-relaxed text-stone-700">{product.description}</p>}
          {details.length > 0 && (
            <dl className="mt-8 grid grid-cols-2 gap-y-2 border-t border-stone-200 pt-6 text-sm">
              {details.map(([label, value]) => (
                <div key={label} className="contents">
                  <dt className="text-stone-500">{label}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          )}
          {/* Phase 3: "Ask Yvette" AI stylist goes here (docs/04). */}
        </div>
      </div>

      {product.completeTheLook.length > 0 && (
        <section className="mt-20">
          <h2 className="mb-6 font-serif text-2xl">Complete the look</h2>
          <ProductGrid products={product.completeTheLook} />
        </section>
      )}

      <RecentlyViewed excludeSlug={product.slug} />
    </>
  );
}

export default function ProductPage({ params }: PageProps<"/products/[slug]">) {
  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <Suspense fallback={<div className="aspect-[4/5] w-full animate-pulse bg-stone-100 md:w-1/2" />}>
        <ProductContent params={params} />
      </Suspense>
    </div>
  );
}
