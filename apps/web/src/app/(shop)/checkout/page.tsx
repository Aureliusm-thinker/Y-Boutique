import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { CheckoutForm, type CheckoutDefaults } from "@/components/CheckoutForm";
import { TrackView } from "@/components/TrackView";
import { getCustomerProfile } from "@/lib/account";
import { getCart } from "@/lib/cart";
import { shopFetch } from "@/lib/shop-api";
import type { CheckoutOptions } from "@/lib/types";

export const metadata: Metadata = { title: "Checkout", robots: { index: false } };

async function CheckoutContent() {
  const [cart, options, profile] = await Promise.all([
    getCart(),
    shopFetch<CheckoutOptions>("/api/v1/checkout/options"),
    getCustomerProfile(),
  ]);
  if (!cart || cart.lines.length === 0) redirect("/cart");
  if (!cart.readyForCheckout) redirect("/cart");

  const address = profile?.addresses.find((a) => a.isDefault) ?? profile?.addresses[0];
  const defaults: CheckoutDefaults = profile
    ? {
        email: profile.email,
        phone: profile.phone ?? undefined,
        fullName: address?.fullName ?? profile.fullName,
        addressLine1: address?.line1,
        addressLine2: address?.line2 ?? undefined,
        city: address?.city,
        county: address?.county,
        postalCode: address?.postalCode ?? undefined,
      }
    : {};

  return (
    <>
      <TrackView name="begin_checkout" properties={{ items: cart.itemCount, value: cart.totalBeforeShipping }} />
      {!profile && (
        <p className="mb-8 text-sm text-stone-600">
          Checking out as a guest.{" "}
          <Link href="/account/login?next=/checkout" className="underline">
            Sign in
          </Link>{" "}
          to use your saved details.
        </p>
      )}
      <CheckoutForm cart={cart} options={options} defaults={defaults} />
    </>
  );
}

export default function CheckoutPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="mb-6 font-serif text-4xl">Checkout</h1>
      <Suspense fallback={<div className="h-96 animate-pulse bg-stone-100" />}>
        <CheckoutContent />
      </Suspense>
    </div>
  );
}
