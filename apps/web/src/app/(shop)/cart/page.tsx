import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { CartLineControls, DiscountForm } from "@/components/CartControls";
import { getCart } from "@/lib/cart";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Your bag", robots: { index: false } };

async function CartContent() {
  const cart = await getCart();
  if (!cart || cart.lines.length === 0) {
    return (
      <div className="py-16 text-center">
        <p className="text-lg">Your bag is empty.</p>
        <Link href="/shop" className="mt-6 inline-block bg-stone-900 px-8 py-3 text-sm uppercase tracking-widest text-white">
          Start shopping
        </Link>
      </div>
    );
  }
  const cheapestDelivery = Math.min(...cart.shippingOptions.filter((o) => o.needsAddress).map((o) => o.price));
  const nextFree = cart.shippingOptions
    .filter((o) => o.freeOver != null && o.price > 0)
    .map((o) => ({ name: o.name, gap: (o.freeOver ?? 0) - cart.totalBeforeShipping }))
    .filter((o) => o.gap > 0)
    .sort((a, b) => a.gap - b.gap)[0];

  return (
    <div className="grid gap-10 lg:grid-cols-[1fr_22rem]">
      <ul className="divide-y divide-stone-200 border-y border-stone-200">
        {cart.lines.map((line) => (
          <li key={line.variantId} className="flex gap-4 py-5">
            <Link href={`/products/${line.productSlug}`} aria-label={line.title} className="relative h-32 w-24 shrink-0 bg-stone-100">
              {line.imageUrl && <Image src={line.imageUrl} alt="" fill sizes="96px" className="object-cover" />}
            </Link>
            <div className="flex flex-1 flex-col">
              <div className="flex justify-between gap-4">
                <div>
                  <Link href={`/products/${line.productSlug}`} className="hover:underline">
                    {line.title}
                  </Link>
                  <p className="text-sm text-stone-500">{[line.size, line.colour].filter(Boolean).join(" · ")}</p>
                </div>
                <p className="text-right">{formatPrice(line.lineTotal, cart.currency)}</p>
              </div>
              {line.problem && <p className="mt-2 text-sm text-red-700">{line.problem}</p>}
              <div className="mt-auto pt-3">
                <CartLineControls variantId={line.variantId} quantity={line.quantity} max={Math.max(1, Math.min(10, line.available))} />
              </div>
            </div>
          </li>
        ))}
      </ul>

      <aside className="h-fit border border-stone-200 bg-white p-6">
        <h2 className="font-serif text-2xl">Summary</h2>
        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt>Subtotal</dt>
            <dd>{formatPrice(cart.subtotal, cart.currency)}</dd>
          </div>
          {cart.discount && cart.discount.amount > 0 && (
            <div className="flex justify-between text-green-800">
              <dt>{cart.discount.name ?? "Discount"}</dt>
              <dd>−{formatPrice(cart.discount.amount, cart.currency)}</dd>
            </div>
          )}
          {cart.discount?.freeShipping && <p className="text-green-800">Free delivery applied</p>}
          <div className="flex justify-between text-stone-500">
            <dt>Delivery</dt>
            <dd>{cheapestDelivery === 0 ? "Free" : `from ${formatPrice(cheapestDelivery, cart.currency)}`}</dd>
          </div>
          <div className="flex justify-between border-t border-stone-200 pt-2 text-base">
            <dt>Total before delivery</dt>
            <dd>{formatPrice(cart.totalBeforeShipping, cart.currency)}</dd>
          </div>
        </dl>
        {nextFree && (
          <p className="mt-3 text-xs text-stone-600">
            Add {formatPrice(nextFree.gap, cart.currency)} more for free {nextFree.name.toLowerCase()}.
          </p>
        )}
        <DiscountForm current={cart.discount?.code ?? null} problem={cart.discount?.problem ?? null} />
        {cart.readyForCheckout ? (
          <Link
            href="/checkout"
            className="mt-6 block bg-stone-900 py-3 text-center text-sm uppercase tracking-widest text-white hover:bg-stone-700"
          >
            Checkout
          </Link>
        ) : (
          <p className="mt-6 text-sm text-red-700">Please fix the items marked above to continue.</p>
        )}
        <p className="mt-3 text-center text-xs text-stone-500">Pay with M-Pesa or card</p>
      </aside>
    </div>
  );
}

export default function CartPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="mb-8 font-serif text-4xl">Your bag</h1>
      <Suspense fallback={<div className="h-40 animate-pulse bg-stone-100" />}>
        <CartContent />
      </Suspense>
    </div>
  );
}
