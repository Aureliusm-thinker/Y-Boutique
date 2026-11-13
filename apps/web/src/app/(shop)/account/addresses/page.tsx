import type { Metadata } from "next";
import { Suspense } from "react";
import { AddressForm, DeleteAddressButton } from "@/components/account/AccountForms";
import { AccountShell } from "@/components/account/AccountShell";
import { requireCustomer } from "@/lib/account";
import { shopFetch } from "@/lib/shop-api";
import type { CheckoutOptions } from "@/lib/types";

export const metadata: Metadata = { title: "Addresses", robots: { index: false } };

async function Content() {
  const [profile, options] = await Promise.all([
    requireCustomer("/account/addresses"),
    shopFetch<CheckoutOptions>("/api/v1/checkout/options"),
  ]);
  return (
    <div className="space-y-10">
      {profile.addresses.length > 0 && (
        <ul className="grid gap-4 sm:grid-cols-2">
          {profile.addresses.map((a) => (
            <li key={a.id} className="border border-stone-200 bg-white p-4 text-sm">
              <p className="font-medium">
                {a.label ?? "Address"} {a.isDefault && <span className="ml-2 text-xs text-stone-500">Default</span>}
              </p>
              <p className="mt-1 text-stone-600">
                {a.fullName}
                <br />
                {a.line1}
                {a.line2 && (
                  <>
                    <br />
                    {a.line2}
                  </>
                )}
                <br />
                {a.city}, {a.county}
              </p>
              <details className="mt-3">
                <summary className="cursor-pointer underline">Edit</summary>
                <div className="mt-3">
                  <AddressForm address={a} counties={options.counties} />
                </div>
              </details>
              <div className="mt-2">
                <DeleteAddressButton id={a.id} />
              </div>
            </li>
          ))}
        </ul>
      )}
      <section>
        <h2 className="mb-4 font-serif text-2xl">Add an address</h2>
        <AddressForm key={profile.addresses.length} counties={options.counties} />
      </section>
    </div>
  );
}

export default function AddressesPage() {
  return (
    <AccountShell title="Addresses" active="/account/addresses">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content />
      </Suspense>
    </AccountShell>
  );
}
