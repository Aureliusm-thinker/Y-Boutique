import Image from "next/image";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { TrackView } from "@/components/TrackView";
import { VariantPicker } from "@/components/VariantPicker";
import { getProduct } from "@/lib/api";

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

  return (
    <div className="grid gap-10 md:grid-cols-2">
      <TrackView name="product_view" properties={{ productId: product.id, slug: product.slug }} />

      <div className="space-y-4">
        {product.images.map((img, i) => (
          <div key={img.url} className="relative aspect-[4/5] bg-stone-100">
            <Image
              src={img.url}
              alt={img.altText ?? product.title}
              fill
              priority={i === 0}
              sizes="(min-width: 768px) 50vw, 100vw"
              className="object-cover"
            />
          </div>
        ))}
      </div>

      <div className="md:sticky md:top-8 md:self-start">
        {product.category && (
          <p className="text-xs uppercase tracking-[0.2em] text-stone-500">{product.category.name}</p>
        )}
        <h1 className="mt-2 font-serif text-4xl">{product.title}</h1>
        <div className="mt-4">
          <VariantPicker variants={product.variants} />
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
        {/* Phase 3: "Ask Yvette" AI stylist and personalised "complete the look" go here. */}
      </div>
    </div>
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
