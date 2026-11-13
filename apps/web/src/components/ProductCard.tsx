import Image from "next/image";
import Link from "next/link";
import { formatPrice } from "@/lib/format";
import type { ProductSummary } from "@/lib/types";

export function ProductCard({ product }: { product: ProductSummary }) {
  const onSale = product.compareAtPrice != null && product.priceFrom != null && product.compareAtPrice > product.priceFrom;
  return (
    <Link href={`/products/${product.slug}`} className="group block">
      <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
        {product.imageUrl && (
          <Image
            src={product.imageUrl}
            alt={product.imageAlt ?? product.title}
            fill
            sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw"
            className="object-cover transition-transform duration-500 group-hover:scale-105"
          />
        )}
        {product.soldOut ? (
          <span className="absolute left-2 top-2 bg-stone-900 px-2 py-0.5 text-xs uppercase tracking-wide text-white">
            Sold out
          </span>
        ) : (
          onSale && <span className="absolute left-2 top-2 bg-white px-2 py-0.5 text-xs uppercase tracking-wide">Sale</span>
        )}
      </div>
      <h3 className="mt-3 text-sm text-stone-900">{product.title}</h3>
      <p className="mt-1 text-sm text-stone-600">
        {formatPrice(product.priceFrom, product.currency)}
        {onSale && (
          <span className="ml-2 text-stone-500 line-through">
            {formatPrice(product.compareAtPrice, product.currency)}
          </span>
        )}
      </p>
    </Link>
  );
}

export function ProductGrid({ products, columns = 4 }: { products: ProductSummary[]; columns?: 3 | 4 }) {
  if (products.length === 0) {
    return <p className="py-12 text-center text-stone-500">No products here yet.</p>;
  }
  return (
    <div className={`grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 ${columns === 4 ? "lg:grid-cols-4" : ""}`}>
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  );
}

export function ProductGridSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: 4 }, (_, i) => (
        <div key={i}>
          <div className="aspect-[4/5] animate-pulse bg-stone-100" />
          <div className="mt-3 h-4 w-2/3 animate-pulse bg-stone-100" />
        </div>
      ))}
    </div>
  );
}
