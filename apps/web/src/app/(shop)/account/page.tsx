import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { AccountShell } from "@/components/account/AccountShell";
import { ResendVerification } from "@/components/account/ResendVerification";
import { statusLabel } from "@/components/OrderDetails";
import { requireCustomer } from "@/lib/account";
import { formatPrice } from "@/lib/format";
import { shopFetch } from "@/lib/shop-api";

export const metadata: Metadata = { title: "My account", robots: { index: false } };

type OrderSummary = {
  number: string;
  accessToken: string;
  status: string;
  paymentStatus: string;
  total: number;
  currency: string;
  itemCount: number;
  placedAt: string;
  firstImageUrl: string | null;
};

async function Orders() {
  const profile = await requireCustomer("/account");
  const orders = await shopFetch<OrderSummary[]>("/api/v1/account/orders");
  return (
    <>
      <p className="mb-6 text-stone-600">Hello, {profile.fullName.split(" ")[0]}.</p>
      {!profile.emailVerified && (
        <div className="mb-8 border border-amber-300 bg-amber-50 p-4 text-sm">
          Please confirm your email address ({profile.email}) — we sent you a link. Orders you placed as a guest
          appear here once it&apos;s confirmed. <ResendVerification />
        </div>
      )}
      {orders.length === 0 ? (
        <p className="text-stone-600">
          No orders yet. <Link href="/shop" className="underline">Start shopping</Link>
        </p>
      ) : (
        <ul className="divide-y divide-stone-200 border-y border-stone-200">
          {orders.map((o) => (
            <li key={o.number}>
              <Link href={`/account/orders/${o.number}`} className="flex items-center gap-4 py-4 hover:bg-stone-50">
                <div className="relative h-20 w-16 shrink-0 bg-stone-100">
                  {o.firstImageUrl && <Image src={o.firstImageUrl} alt="" fill sizes="64px" className="object-cover" />}
                </div>
                <div className="flex-1 text-sm">
                  <p className="font-medium">{o.number}</p>
                  <p className="text-stone-500">
                    {new Date(o.placedAt).toLocaleDateString("en-KE", { dateStyle: "medium" })} · {o.itemCount}{" "}
                    {o.itemCount === 1 ? "item" : "items"}
                  </p>
                </div>
                <div className="text-right text-sm">
                  <p>{formatPrice(o.total, o.currency)}</p>
                  <p className="text-stone-500">{statusLabel[o.status] ?? o.status}</p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export default function AccountPage() {
  return (
    <AccountShell title="My orders" active="/account">
      <Suspense fallback={<div className="h-40 animate-pulse bg-stone-100" />}>
        <Orders />
      </Suspense>
    </AccountShell>
  );
}
