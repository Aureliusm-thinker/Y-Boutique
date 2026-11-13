import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { AccountShell } from "@/components/account/AccountShell";
import { OrderActions } from "@/components/OrderActions";
import { OrderDetails, statusLabel } from "@/components/OrderDetails";
import { requireCustomer } from "@/lib/account";
import { shopFetch, ShopApiError } from "@/lib/shop-api";
import type { Order } from "@/lib/types";

export const metadata: Metadata = { title: "Order", robots: { index: false } };

async function Content({ params }: Pick<PageProps<"/account/orders/[number]">, "params">) {
  const { number } = await params;
  await requireCustomer(`/account/orders/${number}`);
  let order: Order;
  try {
    order = await shopFetch<Order>(`/api/v1/account/orders/${encodeURIComponent(number)}`);
  } catch (e) {
    if (e instanceof ShopApiError && e.status === 404) notFound();
    throw e;
  }
  return (
    <>
      <p className="-mt-6 mb-8 text-stone-600">
        {order.number} · {statusLabel[order.status]}
      </p>
      <OrderDetails order={order} />
      <OrderActions order={order} />
    </>
  );
}

export default function AccountOrderPage({ params }: PageProps<"/account/orders/[number]">) {
  return (
    <AccountShell title="Order details" active="/account">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content params={params} />
      </Suspense>
    </AccountShell>
  );
}
